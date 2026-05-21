package com.gorib.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorib.app.domain.model.Category
import com.gorib.app.domain.model.OcrParseResult
import com.gorib.app.domain.model.Transaction
import com.gorib.app.domain.repository.CategoryRepository
import com.gorib.app.domain.repository.TransactionRepository
import com.gorib.app.domain.repository.UtilityRepository
import com.gorib.app.domain.usecase.GetCurrentRentUseCase
import com.gorib.app.domain.usecase.GetTransactionsByMonthUseCase
import com.gorib.app.domain.usecase.AutoCategorizeUseCase
import com.gorib.app.domain.usecase.ProcessOcrReceiptUseCase
import com.gorib.app.domain.usecase.GetBudgetAlertsUseCase
import com.gorib.app.domain.usecase.BudgetAlert
import com.gorib.app.domain.usecase.AlertLevel
import com.gorib.app.domain.usecase.DeleteTransactionUseCase
import com.gorib.app.domain.usecase.recurring.GetRecurringExpensesUseCase
import com.gorib.app.domain.model.RecurringExpense
import com.gorib.app.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * UI State for the Home dashboard and OCR Simulation, now powered entirely by real Room DB.
 */
data class HomeUiState(
    val recentTransactions: List<Transaction> = emptyList(),
    val totalExpenseMonth: Double = 0.0,
    val totalIncomeMonth: Double = 0.0,
    val unpaidRentAmount: Double = 0.0,
    val unpaidUtilitiesAmount: Double = 0.0,
    val unpaidUtilitiesCount: Int = 0,
    val categorySpent: Map<Long, Double> = emptyMap(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    
    // OCR Simulation State (Implementing the globally locked-in OCR Fallback Rule)
    val showOcrDialog: Boolean = false,
    val ocrRawInputText: String = "",
    val ocrResult: OcrParseResult? = null,
    val currentOcrStep: String = "IDLE", // "IDLE", "SCANNING", "OPTION_C" (Gaps), "OPTION_B" (Manual)
    val showNotification: String? = null,
    val showGeminiApiKeyIntroDialog: Boolean = false,
    val geminiApiKey: String = ""
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val processOcrReceiptUseCase: ProcessOcrReceiptUseCase,
    private val getTransactionsByMonthUseCase: GetTransactionsByMonthUseCase,
    private val getCurrentRentUseCase: GetCurrentRentUseCase,
    private val utilityRepository: UtilityRepository,
    private val categoryRepository: CategoryRepository,
    val autoCategorizeUseCase: AutoCategorizeUseCase,
    private val getBudgetAlertsUseCase: GetBudgetAlertsUseCase,
    private val getRecurringExpensesUseCase: GetRecurringExpensesUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val preferencesRepository: UserPreferencesRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

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

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _budgetAlerts = MutableStateFlow<List<BudgetAlert>>(emptyList())
    val budgetAlerts: StateFlow<List<BudgetAlert>> = _budgetAlerts.asStateFlow()

    val upcomingRecurring: StateFlow<List<RecurringExpense>> = getRecurringExpensesUseCase()
        .map { list ->
            val today = java.time.LocalDate.now().dayOfMonth
            // Show items due within next 5 days
            list.filter { it.dueDay >= today && it.dueDay <= today + 5 }
                .sortedBy { it.dueDay }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    private val _currentMonth = MutableStateFlow(
        java.time.YearMonth.now().toString()
    )
    val currentMonth: StateFlow<String> = _currentMonth.asStateFlow()

    val displayMonth: StateFlow<String> = _currentMonth
        .map { ym ->
            val (y, m) = ym.split("-")
            val monthName = java.time.Month.of(m.toInt())
                .getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault())
            "$monthName $y"
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val canGoForward: StateFlow<Boolean> = _currentMonth
        .map { java.time.YearMonth.parse(it).isBefore(java.time.YearMonth.now()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val unpaidUtilitiesCount: StateFlow<Int> = utilityRepository
        .getUnpaidBillsCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        loadDashboardData()
        checkGeminiIntro()
        viewModelScope.launch {
            preferencesRepository.geminiApiKey.collect { key ->
                _uiState.update { it.copy(geminiApiKey = key) }
            }
        }
    }

    private fun checkGeminiIntro() {
        viewModelScope.launch {
            preferencesRepository.hasShownGeminiIntro.collect { hasShown ->
                if (!hasShown) {
                    _uiState.update { it.copy(showGeminiApiKeyIntroDialog = true) }
                }
            }
        }
    }

    fun dismissGeminiIntro() {
        viewModelScope.launch {
            preferencesRepository.setGeminiIntroShown()
            _uiState.update { it.copy(showGeminiApiKeyIntroDialog = false) }
        }
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            _currentMonth.collect { monthStr ->
                _uiState.update { it.copy(isLoading = true) }
                try {
                    _budgetAlerts.value = getBudgetAlertsUseCase(monthStr)
                } catch (e: Exception) {
                    _budgetAlerts.value = emptyList()
                }
                combine(
                    getTransactionsByMonthUseCase(monthStr),
                    getCurrentRentUseCase(),
                    utilityRepository.getAllUtilities(),
                    categoryRepository.getAllCategories()
                ) { transactions, activeRent, utilities, categoryList ->
                    // 1. Calculate expenses and income
                    val expenses = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                    val income = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }

                    // 2. Map category expenditures
                    val spentMap = transactions
                        .filter { it.type == "EXPENSE" }
                        .groupBy { it.categoryId }
                        .mapValues { entry -> entry.value.sumOf { it.amount } }

                    // 3. Rent notification metrics
                    val rentUnpaid = if (activeRent != null && activeRent.paymentStatus == "UNPAID") {
                        activeRent.amount
                    } else {
                        0.0
                    }

                    // 4. Utility notification metrics
                    val pendingUtilities = utilities.filter { it.group.paymentStatus == "UNPAID" }
                    val utilityUnpaid = pendingUtilities.sumOf { it.group.totalAmount }
                    val utilityCount = pendingUtilities.size

                    HomeUiState(
                        recentTransactions = transactions.take(5),
                        totalExpenseMonth = expenses,
                        totalIncomeMonth = income,
                        unpaidRentAmount = rentUnpaid,
                        unpaidUtilitiesAmount = utilityUnpaid,
                        unpaidUtilitiesCount = utilityCount,
                        categorySpent = spentMap,
                        categories = categoryList,
                        isLoading = false,
                        error = null
                    )
                }.catch { exception ->
                    _uiState.update { it.copy(isLoading = false, error = exception.localizedMessage) }
                }.collect { combinedState ->
                    _uiState.update { 
                        it.copy(
                            recentTransactions = combinedState.recentTransactions,
                            totalExpenseMonth = combinedState.totalExpenseMonth,
                            totalIncomeMonth = combinedState.totalIncomeMonth,
                            unpaidRentAmount = combinedState.unpaidRentAmount,
                            unpaidUtilitiesAmount = combinedState.unpaidUtilitiesAmount,
                            unpaidUtilitiesCount = combinedState.unpaidUtilitiesCount,
                            categorySpent = combinedState.categorySpent,
                            categories = combinedState.categories,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun changeMonth(offset: Int) {
        val current = java.time.YearMonth.parse(_currentMonth.value)
        val now = java.time.YearMonth.now()
        val next = current.plusMonths(offset.toLong())
        // Block future months
        if (next.isAfter(now)) return
        _currentMonth.value = next.toString()
    }

    // --- OCR FALLBACK SIMULATOR LOGIC ---

    fun onOpenOcrDialog() {
        _uiState.update { 
            it.copy(
                showOcrDialog = true, 
                ocrRawInputText = "", 
                ocrResult = null, 
                currentOcrStep = "IDLE"
            ) 
        }
    }

    fun onCloseOcrDialog() {
        _uiState.update { it.copy(showOcrDialog = false) }
    }

    fun onOcrInputTextChanged(text: String) {
        _uiState.update { it.copy(ocrRawInputText = text) }
    }

    fun runOcrEngine() {
        val rawInputText = _uiState.value.ocrRawInputText
        _uiState.update { it.copy(currentOcrStep = "SCANNING") }

        viewModelScope.launch {
            kotlinx.coroutines.delay(1200)
            val parseResult = processOcrReceiptUseCase.execute(rawInputText)
            
            if (parseResult.canProceedWithOptionC()) {
                _uiState.update { 
                    it.copy(
                        ocrResult = parseResult,
                        currentOcrStep = "OPTION_C"
                    ) 
                }
            } else {
                _uiState.update { 
                    it.copy(
                        ocrResult = parseResult,
                        currentOcrStep = "OPTION_B"
                    ) 
                }
            }
        }
    }

    fun runOcrEngineWithImage(uri: String) {
        _uiState.update { it.copy(currentOcrStep = "SCANNING", ocrRawInputText = "Scanned Image: $uri") }
        viewModelScope.launch {
            val parseResult = processOcrReceiptUseCase.executeFromUri(uri, context)
            if (parseResult.canProceedWithOptionC()) {
                _uiState.update { 
                    it.copy(
                        ocrResult = parseResult,
                        currentOcrStep = "OPTION_C"
                    ) 
                }
            } else {
                _uiState.update { 
                    it.copy(
                        ocrResult = parseResult,
                        currentOcrStep = "OPTION_B"
                    ) 
                }
            }
        }
    }

    fun saveOcrTransaction(
        title: String, 
        amount: Double, 
        categoryId: Long, 
        isOcrFallbackApplied: Boolean
    ) {
        viewModelScope.launch {
            val transaction = Transaction(
                id = 0,
                amount = amount,
                title = title,
                description = "Processed via OCR Flow. Fallback: $isOcrFallbackApplied",
                categoryId = categoryId,
                loggedAt = System.currentTimeMillis(),
                type = "EXPENSE",
                isRecurring = false,
                billingMonth = java.time.YearMonth.now().toString(),
                isOcrProcessed = true,
                ocrRawText = _uiState.value.ocrRawInputText,
                ocrStatus = if (isOcrFallbackApplied) "FAILED" else "PARTIAL",
                ocrConfidence = _uiState.value.ocrResult?.confidence
            )

            transactionRepository.addTransaction(transaction)

            _uiState.update { 
                it.copy(
                    showOcrDialog = false,
                    showNotification = "Transaction 'RM ${String.format("%.2f", amount)} - $title' successfully recorded!"
                ) 
            }
            
            kotlinx.coroutines.delay(3000)
            _uiState.update { it.copy(showNotification = null) }
        }
    }

    fun showNotification(message: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(showNotification = message) }
            kotlinx.coroutines.delay(3000)
            _uiState.update { it.copy(showNotification = null) }
        }
    }

    fun dismissNotification() {
        _uiState.update { it.copy(showNotification = null) }
    }
}
