package com.gorib.app.data.repository

import com.gorib.app.data.db.dao.GroceryItemDao
import com.gorib.app.data.db.dao.ShoppingSessionDao
import com.gorib.app.data.db.dao.ShoppingSessionItemDao
import com.gorib.app.data.db.entity.GroceryItemEntity
import com.gorib.app.data.db.entity.ShoppingSessionEntity
import com.gorib.app.data.db.entity.ShoppingSessionItemEntity
import com.gorib.app.data.db.entity.ShoppingSessionWithItems
import com.gorib.app.domain.repository.GroceryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GroceryRepositoryImpl @Inject constructor(
    private val shoppingSessionDao: ShoppingSessionDao,
    private val shoppingSessionItemDao: ShoppingSessionItemDao,
    private val groceryItemDao: GroceryItemDao
) : GroceryRepository {

    override fun getActiveSession(): Flow<ShoppingSessionWithItems?> {
        return shoppingSessionDao.getActiveSessionFlow()
    }

    override fun getSessionHistory(): Flow<List<ShoppingSessionWithItems>> {
        return shoppingSessionDao.getAllSessionsFlow()
    }

    override suspend fun startSession(storeName: String, month: String): Long = withContext(Dispatchers.IO) {
        val session = ShoppingSessionEntity(
            storeName = storeName.trim(),
            billingMonth = month,
            status = "ACTIVE",
            startedAt = System.currentTimeMillis(),
            totalAmount = 0.0
        )
        shoppingSessionDao.insertSession(session)
    }

    override suspend fun endSession(sessionId: Long) = withContext(Dispatchers.IO) {
        val sessionWithItems = shoppingSessionDao.getSessionByIdSync(sessionId) ?: return@withContext
        val total = sessionWithItems.items.sumOf { it.totalPrice }
        val updated = sessionWithItems.session.copy(
            status = "COMPLETED",
            completedAt = System.currentTimeMillis(),
            totalAmount = total
        )
        shoppingSessionDao.updateSession(updated)
    }

    override suspend fun addItem(
        sessionId: Long,
        itemName: String,
        quantity: Double,
        unit: String,
        totalPrice: Double,
        categoryId: Long
    ): Long = withContext(Dispatchers.IO) {
        val nameTrim = itemName.trim()
        val qtyCoerced = if (quantity > 0.0) quantity else 1.0
        val pricePerUnit = totalPrice / qtyCoerced

        val matches = groceryItemDao.searchGroceryItemsSync("%$nameTrim%")
        val matchedItem = matches.firstOrNull { it.name.equals(nameTrim, ignoreCase = true) }
            ?: matches.firstOrNull()

        val item = ShoppingSessionItemEntity(
            sessionId = sessionId,
            itemName = nameTrim,
            quantity = qtyCoerced,
            unit = unit,
            pricePerUnit = pricePerUnit,
            totalPrice = totalPrice,
            categoryId = categoryId,
            groceryItemId = matchedItem?.id
        )

        val itemId = shoppingSessionItemDao.insertItem(item)

        // Update total in real-time
        val sessionWithItems = shoppingSessionDao.getSessionByIdSync(sessionId)
        if (sessionWithItems != null) {
            val currentTotal = sessionWithItems.items.sumOf { it.totalPrice }
            shoppingSessionDao.updateSession(sessionWithItems.session.copy(totalAmount = currentTotal))
        }

        itemId
    }

    override suspend fun removeItem(itemId: Long) = withContext(Dispatchers.IO) {
        val item = shoppingSessionItemDao.getItemByIdSync(itemId) ?: return@withContext
        shoppingSessionItemDao.deleteItem(item)

        val sessionWithItems = shoppingSessionDao.getSessionByIdSync(item.sessionId)
        if (sessionWithItems != null) {
            val newTotal = sessionWithItems.items.sumOf { it.totalPrice }
            shoppingSessionDao.updateSession(sessionWithItems.session.copy(totalAmount = newTotal))
        }
    }

    override suspend fun searchGroceryItems(query: String): List<GroceryItemEntity> = withContext(Dispatchers.IO) {
        val qTrim = query.trim()
        if (qTrim.isEmpty()) return@withContext emptyList()
        groceryItemDao.searchGroceryItemsSync("%$qTrim%")
    }

    override fun getSessionById(sessionId: Long): Flow<ShoppingSessionWithItems?> {
        return shoppingSessionDao.getSessionByIdFlow(sessionId)
    }
}
