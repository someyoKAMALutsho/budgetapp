package com.gorib.app.domain.model.analytics

data class CategorySpending(
    val categoryId: Long,
    val categoryName: String,
    val iconEmoji: String,
    val totalSpent: Double,
    val budgetLimit: Double?,
    val percentage: Double   // totalSpent / grandTotal * 100
)
