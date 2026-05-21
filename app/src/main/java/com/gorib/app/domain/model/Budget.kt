package com.gorib.app.domain.model

/**
 * Domain model representing a Budget.
 */
data class Budget(
    val id: Long,
    val categoryId: Long?,
    val limitAmount: Double,
    val spentAmount: Double,
    val period: String, // "MONTHLY", "WEEKLY"
    val monthYear: String // "YYYY-MM"
)
