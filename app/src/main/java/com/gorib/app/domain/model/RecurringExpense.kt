package com.gorib.app.domain.model

data class RecurringExpense(
    val id: Long,
    val name: String,
    val amountRm: Double,
    val categoryId: Long,
    val categoryName: String,
    val iconEmoji: String,
    val dueDay: Int,
    val isActive: Boolean,
    val note: String?
)
