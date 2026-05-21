package com.gorib.app.domain.model

/**
 * Domain model representing a financial Transaction.
 */
data class Transaction(
    val id: Long,
    val amount: Double, // in RM
    val title: String,
    val description: String?,
    val categoryId: Long,
    val loggedAt: Long,
    val type: String, // "EXPENSE" or "INCOME"
    val isRecurring: Boolean,
    val billingMonth: String = "",
    
    // OCR fields
    val isOcrProcessed: Boolean,
    val ocrRawText: String?,
    val ocrStatus: String, // "NONE", "PARTIAL" (Option C), "FAILED" (Option B manual)
    val ocrConfidence: Double?,
    val receiptPath: String? = null
)
