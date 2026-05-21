package com.gorib.app.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorib.app.domain.model.Category
import com.gorib.app.domain.model.Transaction
import com.gorib.app.domain.repository.CategoryRepository
import com.gorib.app.domain.repository.TransactionRepository
import com.gorib.app.domain.usecase.DeleteTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val deleteTransactionUseCase: DeleteTransactionUseCase
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now().toString())
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    val displayMonth: StateFlow<String> = _selectedMonth.map { ym ->
        val parsed = YearMonth.parse(ym)
        val monthName = parsed.month.getDisplayName(
            java.time.format.TextStyle.FULL, java.util.Locale.getDefault()
        )
        "$monthName ${parsed.year}"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val canGoForward: StateFlow<Boolean> = _selectedMonth.map {
        YearMonth.parse(it).isBefore(YearMonth.now())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var recentlyDeletedTransaction: Transaction? = null

    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        _selectedMonth,
        _searchQuery,
        _selectedCategoryId,
        transactionRepository.getAllTransactions()
    ) { month, query, catId, allList ->
        allList.filter { t ->
            val matchMonth = t.billingMonth == month
            val matchQuery = query.isEmpty() || 
                             t.title.contains(query, ignoreCase = true) || 
                             (t.description?.contains(query, ignoreCase = true) == true)
            val matchCategory = catId == null || t.categoryId == catId
            matchMonth && matchQuery && matchCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun changeMonth(offset: Int) {
        val current = YearMonth.parse(_selectedMonth.value)
        val now = YearMonth.now()
        val next = current.plusMonths(offset.toLong())
        if (next <= now) {
            _selectedMonth.value = next.toString()
        }
    }

    fun setSearch(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(id: Long?) {
        _selectedCategoryId.value = id
    }

    fun deleteTransaction(transaction: Transaction, onUndoAvailable: () -> Unit) {
        viewModelScope.launch {
            deleteTransactionUseCase(transaction)
                .onSuccess {
                    recentlyDeletedTransaction = transaction
                    onUndoAvailable()
                }
        }
    }

    fun undoDelete() {
        val cached = recentlyDeletedTransaction ?: return
        viewModelScope.launch {
            transactionRepository.addTransaction(cached)
            recentlyDeletedTransaction = null
        }
    }
}
