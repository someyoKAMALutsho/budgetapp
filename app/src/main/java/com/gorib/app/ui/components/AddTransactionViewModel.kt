package com.gorib.app.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorib.app.domain.model.Category
import com.gorib.app.domain.model.CategorySuggestion
import com.gorib.app.domain.model.Transaction
import com.gorib.app.domain.usecase.AddTransactionUseCase
import com.gorib.app.domain.usecase.UpdateTransactionUseCase
import com.gorib.app.domain.usecase.AutoCategorizeUseCase
import com.gorib.app.domain.usecase.GetAllCategoriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val autoCategorizeUseCase: AutoCategorizeUseCase,
    private val getAllCategoriesUseCase: GetAllCategoriesUseCase,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val processOcrReceiptUseCase: com.gorib.app.domain.usecase.ProcessOcrReceiptUseCase,
    private val preferencesRepository: com.gorib.app.data.preferences.UserPreferencesRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _suggestedCategory = MutableStateFlow<CategorySuggestion?>(null)
    val suggestedCategory: StateFlow<CategorySuggestion?> = _suggestedCategory.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount.asStateFlow()

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note.asStateFlow()

    private val _receiptUri = MutableStateFlow<String?>(null)
    val receiptUri: StateFlow<String?> = _receiptUri.asStateFlow()

    private val _existingTransaction = MutableStateFlow<Transaction?>(null)
    val existingTransaction: StateFlow<Transaction?> = _existingTransaction.asStateFlow()

    private val _geminiApiKey = MutableStateFlow("")
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    init {
        // Load all categories for the picker
        viewModelScope.launch {
            getAllCategoriesUseCase().collect { cats ->
                _categories.value = cats
                if (cats.isEmpty()) {
                    kotlinx.coroutines.delay(1000)
                    getAllCategoriesUseCase().take(1).collect { retry ->
                        if (retry.isNotEmpty()) _categories.value = retry
                    }
                }
            }
        }
        // Auto-categorize on description change with 300ms debounce
        viewModelScope.launch {
            _description
                .debounce(300L)
                .filter { it.length >= 2 }
                .collect { desc ->
                    // Only auto-categorize if NOT in edit mode to avoid overriding existing choice
                    if (_existingTransaction.value == null) {
                        val suggestion = autoCategorizeUseCase(desc)
                        _suggestedCategory.value = suggestion
                        if (_selectedCategoryId.value == null) {
                            _selectedCategoryId.value = suggestion.categoryId
                        }
                    }
                }
        }
        viewModelScope.launch {
            preferencesRepository.geminiApiKey.collect { key ->
                _geminiApiKey.value = key
            }
        }
    }

    fun setExistingTransaction(transaction: Transaction?) {
        _existingTransaction.value = transaction
        if (transaction != null) {
            _description.value = transaction.title
            _amount.value = "%.2f".format(transaction.amount)
            _selectedCategoryId.value = transaction.categoryId
            _note.value = transaction.description ?: ""
            _receiptUri.value = transaction.receiptPath
        } else {
            resetForm()
        }
    }

    fun onDescriptionChange(value: String) {
        _description.value = value
        if (value.length < 2) _suggestedCategory.value = null
    }

    fun onAmountChange(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' }
        val parts = filtered.split(".")
        _amount.value = if (parts.size <= 2) filtered else _amount.value
    }

    fun onNoteChange(value: String) { _note.value = value }

    fun onCategorySelected(categoryId: Long) {
        _selectedCategoryId.value = categoryId
    }

    fun onReceiptPicked(uri: String?) {
        _receiptUri.value = uri
        if (uri != null) {
            viewModelScope.launch {
                val ocrResult = processOcrReceiptUseCase.executeFromUri(uri, context)
                if (ocrResult.isSuccess) {
                    ocrResult.title?.let { _description.value = it }
                    ocrResult.amount?.let { _amount.value = "%.2f".format(it) }
                    ocrResult.categorySuggestedId?.let { 
                        _selectedCategoryId.value = it
                    }
                    ocrResult.itemsSummary?.let { summary ->
                        val currentNote = _note.value
                        _note.value = if (currentNote.isNotBlank()) "$currentNote\n\n$summary" else summary
                    }
                }
            }
        }
    }

    fun clearSuggestion() { _suggestedCategory.value = null }

    fun resetForm() {
        _description.value = ""
        _amount.value = ""
        _note.value = ""
        _suggestedCategory.value = null
        _selectedCategoryId.value = null
        _receiptUri.value = null
        _existingTransaction.value = null
    }

    fun saveTransaction(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val amountDouble = _amount.value.toDoubleOrNull()
            if (_description.value.isBlank()) {
                onError("Please enter a description"); return@launch
            }
            if (amountDouble == null || amountDouble <= 0) {
                onError("Please enter a valid amount"); return@launch
            }
            val categoryId = _selectedCategoryId.value ?: run {
                onError("Please select a category"); return@launch
            }
            
            // Also save learned user override back
            autoCategorizeUseCase.saveOverride(_description.value, categoryId)

            val currentTx = _existingTransaction.value
            val isEdit = currentTx != null

            val transaction = Transaction(
                id = currentTx?.id ?: 0,
                amount = amountDouble,
                title = _description.value.trim(),
                description = _note.value.trim().ifEmpty { null },
                categoryId = categoryId,
                loggedAt = currentTx?.loggedAt ?: System.currentTimeMillis(),
                type = currentTx?.type ?: "EXPENSE",
                isRecurring = currentTx?.isRecurring ?: false,
                billingMonth = currentTx?.billingMonth ?: java.time.YearMonth.now().toString(),
                isOcrProcessed = currentTx?.isOcrProcessed ?: false,
                ocrRawText = currentTx?.ocrRawText,
                ocrStatus = currentTx?.ocrStatus ?: "NONE",
                ocrConfidence = currentTx?.ocrConfidence,
                receiptPath = _receiptUri.value
            )

            val result = if (isEdit) {
                updateTransactionUseCase(transaction).map { currentTx!!.id }
            } else {
                addTransactionUseCase(transaction)
            }

            result
                .onSuccess {
                    resetForm()
                    onSuccess()
                }
                .onFailure { t ->
                    onError(t.localizedMessage ?: "Failed to save transaction")
                }
        }
    }
}
