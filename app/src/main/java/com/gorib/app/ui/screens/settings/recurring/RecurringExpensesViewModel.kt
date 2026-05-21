package com.gorib.app.ui.screens.settings.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorib.app.domain.model.Category
import com.gorib.app.domain.model.RecurringExpense
import com.gorib.app.domain.repository.CategoryRepository
import com.gorib.app.domain.usecase.AutoCategorizeUseCase
import com.gorib.app.domain.usecase.recurring.AddRecurringExpenseUseCase
import com.gorib.app.domain.usecase.recurring.DeactivateRecurringUseCase
import com.gorib.app.domain.usecase.recurring.GetRecurringExpensesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecurringExpensesUiState(
    val categories: List<Category> = emptyList(),
    val showNotification: String? = null
)

@HiltViewModel
class RecurringExpensesViewModel @Inject constructor(
    private val getRecurringExpensesUseCase: GetRecurringExpensesUseCase,
    private val addRecurringExpenseUseCase: AddRecurringExpenseUseCase,
    private val deactivateRecurringUseCase: DeactivateRecurringUseCase,
    private val categoryRepository: CategoryRepository,
    val autoCategorizeUseCase: AutoCategorizeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecurringExpensesUiState())
    val uiState: StateFlow<RecurringExpensesUiState> = _uiState.asStateFlow()

    val activeRecurringExpenses: StateFlow<List<RecurringExpense>> = getRecurringExpensesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cache the deactivated item to allow 5s Undo snackbar
    private var recentlyDeactivatedExpense: RecurringExpense? = null

    init {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { list ->
                _uiState.value = _uiState.value.copy(categories = list)
            }
        }
    }

    fun addRecurringExpense(
        name: String,
        amount: Double,
        categoryId: Long,
        dueDay: Int,
        note: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                addRecurringExpenseUseCase(name, amount, categoryId, dueDay, note)
                showNotification("Successfully added recurring expense '$name'")
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to add recurring expense")
            }
        }
    }

    fun deactivateExpense(expense: RecurringExpense) {
        recentlyDeactivatedExpense = expense
        viewModelScope.launch {
            try {
                deactivateRecurringUseCase(expense.id)
                showNotification("Deactivated '${expense.name}'")
            } catch (e: Exception) {
                showNotification("Failed to deactivate: ${e.localizedMessage}")
            }
        }
    }

    fun undoDeactivate() {
        val expense = recentlyDeactivatedExpense ?: return
        viewModelScope.launch {
            try {
                // Re-add or re-activate the same
                addRecurringExpenseUseCase(
                    name = expense.name,
                    amount = expense.amountRm,
                    categoryId = expense.categoryId,
                    dueDay = expense.dueDay,
                    note = expense.note
                )
                recentlyDeactivatedExpense = null
                showNotification("Undo successful ✓")
            } catch (e: Exception) {
                showNotification("Undo failed: ${e.localizedMessage}")
            }
        }
    }

    fun showNotification(message: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(showNotification = message)
            kotlinx.coroutines.delay(5000)
            if (_uiState.value.showNotification == message) {
                _uiState.value = _uiState.value.copy(showNotification = null)
            }
        }
    }

    fun dismissNotification() {
        _uiState.value = _uiState.value.copy(showNotification = null)
    }
}
