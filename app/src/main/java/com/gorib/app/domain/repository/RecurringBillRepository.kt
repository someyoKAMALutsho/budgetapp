package com.gorib.app.domain.repository

import com.gorib.app.domain.model.RecurringBill
import kotlinx.coroutines.flow.Flow

/**
 * Domain-level Repository interface for Recurring Bill operations.
 */
interface RecurringBillRepository {
    fun getAllRecurringBills(): Flow<List<RecurringBill>>
    fun getUnpaidRecurringBills(): Flow<List<RecurringBill>>
    suspend fun addRecurringBill(recurringBill: RecurringBill): Long
    suspend fun updateRecurringBill(recurringBill: RecurringBill)
    suspend fun updatePaidStatus(id: Long, isPaid: Boolean)
    suspend fun deleteRecurringBill(recurringBill: RecurringBill)
}
