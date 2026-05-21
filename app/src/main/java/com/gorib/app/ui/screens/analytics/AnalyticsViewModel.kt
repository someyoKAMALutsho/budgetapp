package com.gorib.app.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorib.app.domain.model.analytics.MonthlySpendingSummary
import com.gorib.app.domain.model.analytics.MonthComparisonResult
import com.gorib.app.domain.usecase.analytics.GetMonthlySpendingUseCase
import com.gorib.app.domain.usecase.analytics.GetMonthComparisonUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val getMonthlySpending: GetMonthlySpendingUseCase,
    private val getMonthComparison: GetMonthComparisonUseCase
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(
        java.time.YearMonth.now().toString()
    )

    val displayMonth: StateFlow<String> = _selectedMonth.map { ym ->
        val parsed = java.time.YearMonth.parse(ym)
        val monthName = parsed.month.getDisplayName(
            java.time.format.TextStyle.FULL, java.util.Locale.getDefault()
        )
        "$monthName ${parsed.year}"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val canGoForward: StateFlow<Boolean> = _selectedMonth.map {
        java.time.YearMonth.parse(it).isBefore(java.time.YearMonth.now())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _summary = MutableStateFlow<MonthlySpendingSummary?>(null)
    val summary: StateFlow<MonthlySpendingSummary?> = _summary.asStateFlow()

    private val _comparison = MutableStateFlow<MonthComparisonResult?>(null)
    val comparison: StateFlow<MonthComparisonResult?> = _comparison.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _comparisonExpanded = MutableStateFlow(false)
    val comparisonExpanded: StateFlow<Boolean> = _comparisonExpanded.asStateFlow()

    init {
        viewModelScope.launch {
            _selectedMonth.collect { month -> loadData(month) }
        }
    }

    private suspend fun loadData(month: String) {
        _isLoading.value = true
        try {
            val prev = java.time.YearMonth.parse(month).minusMonths(1).toString()
            _summary.value = getMonthlySpending(month)
            _comparison.value = getMonthComparison(month, prev)
        } catch (e: Exception) {
            // silently keep previous data on error
        } finally {
            _isLoading.value = false
        }
    }

    fun changeMonth(offset: Int) {
        val current = java.time.YearMonth.parse(_selectedMonth.value)
        val now = java.time.YearMonth.now()
        val next = current.plusMonths(offset.toLong())
        if (next.isAfter(now)) return
        _selectedMonth.value = next.toString()
    }

    fun toggleComparisonExpanded() {
        _comparisonExpanded.value = !_comparisonExpanded.value
    }
}
