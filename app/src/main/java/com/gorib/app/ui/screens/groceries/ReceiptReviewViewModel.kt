package com.gorib.app.ui.screens.groceries

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorib.app.data.db.dao.CategoryDao
import com.gorib.app.domain.model.ParsedReceipt
import com.gorib.app.domain.model.OcrReceiptParser
import com.gorib.app.domain.usecase.grocery.AddSessionItemUseCase
import com.gorib.app.domain.usecase.AddTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReceiptReviewViewModel @Inject constructor(
    private val addSessionItemUseCase: AddSessionItemUseCase,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val categoryDao: CategoryDao
) : ViewModel() {

    private val _parsedReceipt = MutableStateFlow<ParsedReceipt?>(null)

    // Editable list of review items
    private val _reviewItems = MutableStateFlow<List<ReviewItem>>(emptyList())
    val reviewItems: StateFlow<List<ReviewItem>> = _reviewItems.asStateFlow()

    val storeName = MutableStateFlow("")
    val grandTotal = MutableStateFlow(0.0)
    val rawOcrText = MutableStateFlow<String?>(null)

    // Session context (set when coming from Groceries tab)
    var activeSessionId: Long? = null
    // Transaction context (set when coming from Add Expense receipt attach)
    var isStandaloneTransaction: Boolean = false

    data class ReviewItem(
        val id: Int,  // local index
        val rawLine: String,
        var name: String,
        var quantity: Double,
        var unit: String,
        var totalPrice: Double,
        val confidence: Float,
        val needsReview: Boolean  // confidence < 0.7
    )

    fun loadParsedReceipt(receipt: ParsedReceipt, sessionId: Long?, rawText: String? = null) {
        activeSessionId = sessionId
        isStandaloneTransaction = sessionId == null
        storeName.value = receipt.storeName ?: ""
        grandTotal.value = receipt.grandTotal ?: 0.0
        rawOcrText.value = rawText
        _reviewItems.value = receipt.items.mapIndexed { i, item ->
            ReviewItem(
                id = i,
                rawLine = item.rawLine,
                name = item.name ?: "",
                quantity = item.quantity ?: 1.0,
                unit = item.unit ?: "pcs",
                totalPrice = item.totalPrice ?: 0.0,
                confidence = item.confidence,
                needsReview = item.confidence < 0.70f
            )
        }
        _savedSuccessfully.value = false
        _isSaving.value = false
    }

    fun updateItem(id: Int, name: String? = null, qty: Double? = null,
                   unit: String? = null, price: Double? = null) {
        _reviewItems.update { list ->
            list.map { item ->
                if (item.id == id) item.copy(
                    name = name ?: item.name,
                    quantity = qty ?: item.quantity,
                    unit = unit ?: item.unit,
                    totalPrice = price ?: item.totalPrice
                ) else item
            }
        }
    }

    fun removeItem(id: Int) {
        _reviewItems.update { it.filter { item -> item.id != id } }
    }

    val calculatedTotal: StateFlow<Double> = _reviewItems
        .map { it.sumOf { item -> item.totalPrice } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _savedSuccessfully = MutableStateFlow(false)
    val savedSuccessfully: StateFlow<Boolean> = _savedSuccessfully.asStateFlow()

    fun approveAndSave() {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val items = _reviewItems.value.filter { it.totalPrice > 0 }
                val groceriesCategoryId = categoryDao.getCategoryByName("Groceries")?.id ?: 1L

                if (activeSessionId != null) {
                    // Save each item into the active grocery session
                    items.forEach { item ->
                        addSessionItemUseCase(
                            sessionId = activeSessionId!!,
                            itemName = item.name.ifBlank { "Item ${item.id + 1}" },
                            quantity = item.quantity,
                            unit = item.unit,
                            totalPrice = item.totalPrice,
                            categoryId = groceriesCategoryId
                        )
                    }
                } else {
                    // Standalone: create one transaction for the total
                    val total = if (grandTotal.value > 0)
                        grandTotal.value
                    else items.sumOf { it.totalPrice }

                    val transaction = com.gorib.app.domain.model.Transaction(
                        id = 0,
                        amount = total,
                        title = storeName.value.ifBlank { "Receipt Scan" },
                        description = "${items.size} items scanned from receipt",
                        categoryId = groceriesCategoryId,
                        loggedAt = System.currentTimeMillis(),
                        type = "EXPENSE",
                        isRecurring = false,
                        billingMonth = java.time.YearMonth.now().toString(),
                        isOcrProcessed = true,
                        ocrRawText = rawOcrText.value,
                        ocrStatus = "COMPLETE",
                        ocrConfidence = null,
                        receiptPath = null
                    )
                    addTransactionUseCase(transaction)
                }
                _savedSuccessfully.value = true
            } catch (e: Exception) {
                // surface error
                e.printStackTrace()
            } finally {
                _isSaving.value = false
            }
        }
    }
}
