package com.gorib.app.domain.repository

import com.gorib.app.data.db.entity.UtilityBillGroupWithItems
import kotlinx.coroutines.flow.Flow

/**
 * Domain-level Repository interface for Utility Bill operations.
 */
interface UtilityRepository {
    fun getUtilitiesForMonth(month: String): Flow<List<UtilityBillGroupWithItems>>
    fun getAllUtilities(): Flow<List<UtilityBillGroupWithItems>>
    suspend fun saveSingleBill(
        type: String, 
        customName: String?, 
        amount: Double,
        month: String, 
        note: String?, 
        receiptPath: String?
    ): Long
    suspend fun saveCombinedBill(
        lineItems: List<UtilityLineItemInput>, 
        month: String,
        note: String?, 
        receiptPath: String?
    ): Long
    suspend fun markAsPaid(groupId: Long)
    suspend fun markAsUnpaid(groupId: Long)
    suspend fun deleteBill(groupId: Long)
    fun getUnpaidBillsCount(): Flow<Int>
}

/**
 * Input transfer object representing an individual utility line item when saving a combined bill.
 */
data class UtilityLineItemInput(
    val utilityType: String,
    val customName: String?,
    val amount: Double
)
