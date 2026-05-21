package com.gorib.app.domain.repository

import com.gorib.app.domain.model.Budget
import kotlinx.coroutines.flow.Flow

/**
 * Domain-level Repository interface for Budget operations.
 */
interface BudgetRepository {
    fun getBudgetsForMonth(monthYear: String): Flow<List<Budget>>
    suspend fun getBudgetByCategoryForMonth(categoryId: Long, monthYear: String): Budget?
    suspend fun getGlobalBudgetForMonth(monthYear: String): Budget?
    suspend fun addBudget(budget: Budget): Long
    suspend fun updateBudget(budget: Budget)
    suspend fun incrementBudgetSpend(categoryId: Long, monthYear: String, amount: Double)
    suspend fun incrementGlobalBudgetSpend(monthYear: String, amount: Double)
    suspend fun deleteBudget(budget: Budget)
}
