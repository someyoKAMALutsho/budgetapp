package com.gorib.app.domain.repository

import com.gorib.app.domain.model.Category
import kotlinx.coroutines.flow.Flow

/**
 * Domain-level Repository interface for Category operations.
 */
interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    suspend fun getCategoryById(id: Long): Category?
    fun getCategoriesByType(type: String): Flow<List<Category>>
    suspend fun addCategory(category: Category): Long
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(category: Category)
    suspend fun updateBudget(id: Long, budget: Double?)
}
