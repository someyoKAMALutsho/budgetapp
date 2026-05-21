package com.gorib.app.domain.model

/**
 * Domain model representing a category auto-categorization suggestion.
 */
data class CategorySuggestion(
    val categoryId: Long,
    val categoryName: String,
    val confidence: Float
)
