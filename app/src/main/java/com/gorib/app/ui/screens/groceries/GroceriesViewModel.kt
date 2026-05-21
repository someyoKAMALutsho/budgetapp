package com.gorib.app.ui.screens.groceries

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorib.app.data.db.entity.GroceryItemEntity
import com.gorib.app.data.db.entity.ShoppingSessionWithItems
import com.gorib.app.domain.model.Transaction
import com.gorib.app.domain.repository.CategoryRepository
import com.gorib.app.domain.usecase.AddTransactionUseCase
import com.gorib.app.domain.usecase.grocery.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.gorib.app.domain.model.Category
import com.gorib.app.data.preferences.UserPreferencesRepository
import javax.inject.Inject

data class GroceriesUiState(
    val activeSession: ShoppingSessionWithItems? = null,
    val sessionHistory: List<ShoppingSessionWithItems> = emptyList(),
    val autocompleteResults: List<GroceryItemEntity> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showStartSheet: Boolean = false,
    val showAddItemSheet: Boolean = false,
    val showEndSessionSheet: Boolean = false,
    val geminiApiKey: String = ""
)

@OptIn(FlowPreview::class)
@HiltViewModel
class GroceriesViewModel @Inject constructor(
    private val getActiveSessionUseCase: GetActiveSessionUseCase,
    private val getSessionHistoryUseCase: GetSessionHistoryUseCase,
    private val searchGroceryItemsUseCase: SearchGroceryItemsUseCase,
    private val startShoppingSessionUseCase: StartShoppingSessionUseCase,
    private val endShoppingSessionUseCase: EndShoppingSessionUseCase,
    private val addSessionItemUseCase: AddSessionItemUseCase,
    private val removeSessionItemUseCase: RemoveSessionItemUseCase,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val categoryRepository: CategoryRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroceriesUiState())
    val uiState: StateFlow<GroceriesUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        // Observe categories
        viewModelScope.launch {
            categoryRepository.getAllCategories()
                .catch { e -> _uiState.update { it.copy(errorMessage = e.localizedMessage) } }
                .collect { cats ->
                    _uiState.update { it.copy(categories = cats) }
                }
        }

        // Observe active session
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getActiveSessionUseCase()
                .catch { e -> _uiState.update { it.copy(errorMessage = e.localizedMessage, isLoading = false) } }
                .collect { active ->
                    _uiState.update { it.copy(activeSession = active, isLoading = false) }
                }
        }

        // Observe session history
        viewModelScope.launch {
            getSessionHistoryUseCase()
                .catch { e -> _uiState.update { it.copy(errorMessage = e.localizedMessage) } }
                .collect { history ->
                    val completed = history.filter { it.session.status == "COMPLETED" }
                    _uiState.update { it.copy(sessionHistory = completed) }
                }
        }

        // Autocomplete with 200ms debounce
        viewModelScope.launch {
            _searchQuery
                .debounce(200)
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    flow {
                        if (query.length >= 2) {
                            emit(searchGroceryItemsUseCase(query))
                        } else {
                            emit(emptyList())
                        }
                    }
                }
                .catch { e -> _uiState.update { it.copy(errorMessage = e.localizedMessage) } }
                .collect { results ->
                    _uiState.update { it.copy(autocompleteResults = results) }
                }
        }

        // Observe Gemini API Key
        viewModelScope.launch {
            preferencesRepository.geminiApiKey.collect { key ->
                _uiState.update { it.copy(geminiApiKey = key) }
            }
        }
    }

    fun startSession(storeName: String) {
        if (storeName.isBlank()) return
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                val month = java.time.YearMonth.now().toString() // YYYY-MM
                startShoppingSessionUseCase(storeName.trim(), month)
                // Do NOT navigate here — active session Flow will auto-update UI
                _uiState.update { it.copy(isLoading = false, showStartSheet = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to start session: ${e.message}") }
            }
        }
    }

    fun endSession(onSuccess: (Double) -> Unit = {}) {
        val active = _uiState.value.activeSession ?: return
        val sessionId = active.session.id
        val storeName = active.session.storeName
        val total = active.items.sumOf { it.totalPrice }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // 1. Mark session ended
                endShoppingSessionUseCase(sessionId)

                // 2. Query category dynamically by name "Groceries"
                val allCats = categoryRepository.getAllCategories().firstOrNull()
                val groceriesCategory = allCats?.firstOrNull { it.name.equals("Groceries", ignoreCase = true) }

                // 3. Create the auto Transaction
                val transaction = Transaction(
                    id = 0,
                    amount = total,
                    title = "Groceries - $storeName",
                    description = null,
                    categoryId = groceriesCategory?.id ?: 1L,
                    loggedAt = System.currentTimeMillis(),
                    type = "EXPENSE",
                    isRecurring = false,
                    billingMonth = java.time.YearMonth.now().toString(),
                    isOcrProcessed = false,
                    ocrRawText = null,
                    ocrStatus = "NONE",
                    ocrConfidence = null
                )
                addTransactionUseCase(transaction)

                _uiState.update { it.copy(isLoading = false, showEndSessionSheet = false, errorMessage = null) }
                onSuccess(total)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    fun addItem(name: String, quantity: Double, unit: String, totalPrice: Double, categoryId: Long) {
        val active = _uiState.value.activeSession ?: return
        viewModelScope.launch {
            try {
                addSessionItemUseCase(active.session.id, name, quantity, unit, totalPrice, categoryId)
                _searchQuery.value = "" // reset autocomplete
                _uiState.update { it.copy(showAddItemSheet = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.localizedMessage) }
            }
        }
    }

    fun removeItem(itemId: Long) {
        viewModelScope.launch {
            try {
                removeSessionItemUseCase(itemId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.localizedMessage) }
            }
        }
    }

    fun searchItems(query: String) {
        _searchQuery.value = query
    }

    fun clearAutocomplete() {
        _searchQuery.value = ""
        _uiState.update { it.copy(autocompleteResults = emptyList()) }
    }

    fun showStartSheet(show: Boolean) {
        _uiState.update { it.copy(showStartSheet = show) }
    }

    fun showAddItemSheet(show: Boolean) {
        _uiState.update { it.copy(showAddItemSheet = show) }
    }

    fun showEndSessionSheet(show: Boolean) {
        _uiState.update { it.copy(showEndSessionSheet = show) }
    }
}
