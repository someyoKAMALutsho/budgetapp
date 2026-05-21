package com.gorib.app.ui.screens.category

import androidx.lifecycle.SavedStateHandle
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
class CategoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val deleteTransactionUseCase: DeleteTransactionUseCase
) : ViewModel() {

    val categoryId: Long = checkNotNull(savedStateHandle["categoryId"])

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

    val category: StateFlow<Category?> = categoryRepository.getAllCategories()
        .map { list -> list.find { it.id == categoryId } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val transactions: StateFlow<List<Transaction>> = _selectedMonth
        .flatMapLatest { month ->
            transactionRepository.getTransactionsByCategoryAndMonth(categoryId, month)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalSpent: StateFlow<Double> = transactions
        .map { list -> list.sumOf { it.amount } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    fun changeMonth(offset: Int) {
        val current = YearMonth.parse(_selectedMonth.value)
        val now = YearMonth.now()
        val next = current.plusMonths(offset.toLong())
        if (next <= now) {
            _selectedMonth.value = next.toString()
        }
    }

    fun deleteTransaction(transaction: Transaction, onUndoAvailable: (suspend () -> Unit) -> Unit) {
        viewModelScope.launch {
            deleteTransactionUseCase(transaction)
                .onSuccess {
                    onUndoAvailable {
                        transactionRepository.addTransaction(transaction)
                    }
                }
        }
    }
}
