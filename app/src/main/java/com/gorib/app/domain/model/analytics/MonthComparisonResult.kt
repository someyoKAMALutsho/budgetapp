package com.gorib.app.domain.model.analytics

data class MonthComparisonResult(
    val currentTotal: Double,
    val previousTotal: Double,
    val changePercent: Double,
    val categoryChanges: List<CategoryChange>
)
