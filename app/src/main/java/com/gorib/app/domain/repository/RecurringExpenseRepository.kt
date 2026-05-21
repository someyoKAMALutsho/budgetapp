package com.gorib.app.domain.repository

import com.gorib.app.domain.model.RecurringExpense
import kotlinx.coroutines.flow.Flow

interface RecurringExpenseRepository {
    fun getActiveRecurring(): Flow<List<RecurringExpense>>
    suspend fun addRecurring(
        name: String, amount: Double,
        categoryId: Long, dueDay: Int,
        note: String?
    ): Long
    suspend fun updateRecurring(expense: RecurringExpense)
    suspend fun deactivate(id: Long)
}
