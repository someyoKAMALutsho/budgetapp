package com.gorib.app.data.repository

import com.gorib.app.data.db.dao.RentConfigDao
import com.gorib.app.data.db.entity.RentConfigEntity
import com.gorib.app.domain.repository.RentConfigRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [RentConfigRepository] utilizing Room [RentConfigDao].
 */
@Singleton
class RentConfigRepositoryImpl @Inject constructor(
    private val rentConfigDao: RentConfigDao
) : RentConfigRepository {

    override fun getCurrentRent(): Flow<RentConfigEntity?> {
        return rentConfigDao.getCurrentRent().flowOn(Dispatchers.IO)
    }

    override fun getRentHistory(): Flow<List<RentConfigEntity>> {
        return rentConfigDao.getRentHistory().flowOn(Dispatchers.IO)
    }

    override suspend fun saveRent(amount: Double, note: String?, effectiveFrom: String): Long = withContext(Dispatchers.IO) {
        if (amount <= 0.0) {
            throw IllegalArgumentException("Rent amount must be greater than RM 0")
        }
        val config = RentConfigEntity(
            amount = amount,
            effectiveFrom = effectiveFrom,
            paymentStatus = "UNPAID",
            paidOnDate = null,
            note = note
        )
        rentConfigDao.insertOrUpdateRentConfig(config)
    }

    override suspend fun markAsPaid(rentId: Long) = withContext(Dispatchers.IO) {
        val config = rentConfigDao.getRentById(rentId)
        if (config != null) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val updated = config.copy(
                paymentStatus = "PAID",
                paidOnDate = sdf.format(Date())
            )
            rentConfigDao.updateRentConfig(updated)
        }
    }

    override suspend fun markAsUnpaid(rentId: Long) = withContext(Dispatchers.IO) {
        val config = rentConfigDao.getRentById(rentId)
        if (config != null) {
            val updated = config.copy(
                paymentStatus = "UNPAID",
                paidOnDate = null
            )
            rentConfigDao.updateRentConfig(updated)
        }
    }
}
