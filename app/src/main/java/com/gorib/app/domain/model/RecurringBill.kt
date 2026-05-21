package com.gorib.app.domain.model

/**
 * Domain model representing a Recurring Bill or Subscription scheduling.
 */
data class RecurringBill(
    val id: Long,
    val title: String,
    val dueDate: Long,
    val categoryId: Long,
    val recurrencePeriod: String, // "DAILY", "WEEKLY", "MONTHLY", "YEARLY"
    val isPaid: Boolean,
    val autoPay: Boolean
)
