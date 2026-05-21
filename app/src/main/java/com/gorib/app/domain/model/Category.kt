package com.gorib.app.domain.model

/**
 * Domain model representing a transaction Category.
 */
data class Category(
    val id: Long,
    val name: String,
    val iconEmoji: String,
    val type: String,
    val isSystem: Boolean,
    val sortOrder: Int,
    val monthlyBudgetRm: Double? = null
)
