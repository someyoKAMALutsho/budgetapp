package com.gorib.app.domain.model.analytics

data class CategoryChange(
    val categoryName: String,
    val iconEmoji: String,
    val currentSpent: Double,
    val previousSpent: Double,
    val changePercent: Double  // ((current - previous) / previous) * 100
                               // 0.0 if previousSpent == 0
)
