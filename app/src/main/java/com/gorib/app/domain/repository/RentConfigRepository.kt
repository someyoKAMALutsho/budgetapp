package com.gorib.app.domain.repository

import com.gorib.app.data.db.entity.RentConfigEntity
import kotlinx.coroutines.flow.Flow

/**
 * Domain-level Repository interface for Rent Configuration operations.
 */
interface RentConfigRepository {
    fun getCurrentRent(): Flow<RentConfigEntity?>
    fun getRentHistory(): Flow<List<RentConfigEntity>>
    suspend fun saveRent(amount: Double, note: String?, effectiveFrom: String): Long
    suspend fun markAsPaid(rentId: Long)
    suspend fun markAsUnpaid(rentId: Long)
}
