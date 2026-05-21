package com.gorib.app.domain.model

/**
 * Domain model representing the results of a scanned receipt.
 * Models the globally locked-in OCR Fallback Rule.
 */
data class OcrParseResult(
    val isSuccess: Boolean, // False if nothing parseable -> triggers Option B
    val title: String?,
    val amount: Double?, // in MYR (RM)
    val categorySuggestedId: Long?,
    val confidence: Double,
    val rawText: String,
    val gaps: List<String>, // Fields that are missing or uncertain (e.g., "amount", "category") -> Option C
    val itemsSummary: String? = null // Formatted string listing all extracted items and their prices
) {
    /**
     * Determines if the result is parseable enough to present to the user (Option C).
     * If absolutely nothing can be extracted, we skip to Option B (full manual).
     */
    fun canProceedWithOptionC(): Boolean {
        return isSuccess && (title != null || amount != null)
    }
}
