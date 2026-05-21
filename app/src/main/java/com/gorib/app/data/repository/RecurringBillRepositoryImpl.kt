package com.gorib.app.data.repository

import com.gorib.app.data.db.dao.RecurringBillDao
import com.gorib.app.data.db.entity.RecurringBillEntity
import com.gorib.app.domain.model.RecurringBill
import com.gorib.app.domain.repository.RecurringBillRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [RecurringBillRepository] utilizing Room [RecurringBillDao].
 */
@Singleton
class RecurringBillRepositoryImpl @Inject constructor(
    private val recurringBillDao: RecurringBillDao
) : RecurringBillRepository {

    override fun getAllRecurringBills(): Flow<List<RecurringBill>> {
        return recurringBillDao.getAllRecurringBills().map { list ->
            list.map { it.toDomain() }
        }.flowOn(Dispatchers.IO)
    }

    override fun getUnpaidRecurringBills(): Flow<List<RecurringBill>> {
        return recurringBillDao.getUnpaidRecurringBills().map { list ->
            list.map { it.toDomain() }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun addRecurringBill(recurringBill: RecurringBill): Long = withContext(Dispatchers.IO) {
        recurringBillDao.insertRecurringBill(recurringBill.toEntity())
    }

    override suspend fun updateRecurringBill(recurringBill: RecurringBill) = withContext(Dispatchers.IO) {
        recurringBillDao.updateRecurringBill(recurringBill.toEntity())
    }

    override suspend fun updatePaidStatus(id: Long, isPaid: Boolean) = withContext(Dispatchers.IO) {
        recurringBillDao.updatePaidStatus(id, isPaid)
    }

    override suspend fun deleteRecurringBill(recurringBill: RecurringBill) = withContext(Dispatchers.IO) {
        recurringBillDao.deleteRecurringBill(recurringBill.toEntity())
    }

    // Mapper extensions
    private fun RecurringBillEntity.toDomain(): RecurringBill {
        return RecurringBill(id, title, dueDate, categoryId, recurrencePeriod, isPaid, autoPay)
    }

    private fun RecurringBill.toEntity(): RecurringBillEntity {
        return RecurringBillEntity(id, title, dueDate, categoryId, recurrencePeriod, isPaid, autoPay)
    }
}
