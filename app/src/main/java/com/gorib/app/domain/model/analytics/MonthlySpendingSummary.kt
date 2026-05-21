package com.gorib.app.domain.model.analytics

data class MonthlySpendingSummary(
    val totalSpent: Double,
    val totalBudget: Double,   // sum of all category budgets (0.0 if none set)
    val byCategory: List<CategorySpending>,
    val dailyTotals: List<DailySpending>,
    val topMerchants: List<MerchantSpending>
)
