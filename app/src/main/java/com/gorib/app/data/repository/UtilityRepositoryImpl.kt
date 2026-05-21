package com.gorib.app.data.repository

import com.gorib.app.data.db.dao.UtilityBillGroupDao
import com.gorib.app.data.db.entity.UtilityBillGroupEntity
import com.gorib.app.data.db.entity.UtilityBillGroupWithItems
import com.gorib.app.data.db.entity.UtilityBillLineItemEntity
import com.gorib.app.domain.repository.UtilityLineItemInput
import com.gorib.app.domain.repository.UtilityRepository
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
 * Implementation of [UtilityRepository] utilizing [UtilityBillGroupDao].
 */
@Singleton
class UtilityRepositoryImpl @Inject constructor(
    private val utilityBillGroupDao: UtilityBillGroupDao
) : UtilityRepository {

    override fun getUtilitiesForMonth(month: String): Flow<List<UtilityBillGroupWithItems>> {
        return utilityBillGroupDao.getGroupsWithItemsForMonth(month).flowOn(Dispatchers.IO)
    }

    override fun getAllUtilities(): Flow<List<UtilityBillGroupWithItems>> {
        return utilityBillGroupDao.getAllGroupsWithItems().flowOn(Dispatchers.IO)
    }

    override suspend fun saveSingleBill(
        type: String,
        customName: String?,
        amount: Double,
        month: String,
        note: String?,
        receiptPath: String?
    ): Long = withContext(Dispatchers.IO) {
        val displayName = customName ?: type
        val group = UtilityBillGroupEntity(
            name = displayName,
            billingMonth = month,
            paymentStatus = "UNPAID",
            paidOnDate = null,
            isCombined = false,
            totalAmount = amount,
            note = note,
            receiptPath = receiptPath
        )
        val groupId = utilityBillGroupDao.insertUtilityBillGroup(group)

        val lineItem = UtilityBillLineItemEntity(
            groupId = groupId,
            type = type,
            customName = customName,
            amount = amount
        )
        utilityBillGroupDao.insertLineItems(listOf(lineItem))
        groupId
    }

    override suspend fun saveCombinedBill(
        lineItems: List<UtilityLineItemInput>,
        month: String,
        note: String?,
        receiptPath: String?
    ): Long = withContext(Dispatchers.IO) {
        val total = lineItems.sumOf { it.amount }
        val group = UtilityBillGroupEntity(
            name = "Combined Utility Bill",
            billingMonth = month,
            paymentStatus = "UNPAID",
            paidOnDate = null,
            isCombined = true,
            totalAmount = total,
            note = note,
            receiptPath = receiptPath
        )
        val groupId = utilityBillGroupDao.insertUtilityBillGroup(group)

        val entities = lineItems.map {
            UtilityBillLineItemEntity(
                groupId = groupId,
                type = it.utilityType,
                customName = it.customName,
                amount = it.amount
            )
        }
        utilityBillGroupDao.insertLineItems(entities)
        groupId
    }

    override suspend fun markAsPaid(groupId: Long) = withContext(Dispatchers.IO) {
        val group = utilityBillGroupDao.getGroupById(groupId)
        if (group != null) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val updated = group.copy(
                paymentStatus = "PAID",
                paidOnDate = sdf.format(Date())
            )
            utilityBillGroupDao.updateUtilityBillGroup(updated)
        }
    }

    override suspend fun markAsUnpaid(groupId: Long) = withContext(Dispatchers.IO) {
        val group = utilityBillGroupDao.getGroupById(groupId)
        if (group != null) {
            val updated = group.copy(
                paymentStatus = "UNPAID",
                paidOnDate = null
            )
            utilityBillGroupDao.updateUtilityBillGroup(updated)
        }
    }

    override suspend fun deleteBill(groupId: Long) = withContext(Dispatchers.IO) {
        val group = utilityBillGroupDao.getGroupById(groupId)
        if (group != null) {
            utilityBillGroupDao.deleteUtilityBillGroup(group)
        }
    }

    override fun getUnpaidBillsCount(): Flow<Int> {
        return utilityBillGroupDao.getUnpaidCount().flowOn(Dispatchers.IO)
    }
}
