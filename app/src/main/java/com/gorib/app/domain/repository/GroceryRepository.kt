package com.gorib.app.domain.repository

import com.gorib.app.data.db.entity.GroceryItemEntity
import com.gorib.app.data.db.entity.ShoppingSessionWithItems
import kotlinx.coroutines.flow.Flow

interface GroceryRepository {
    fun getActiveSession(): Flow<ShoppingSessionWithItems?>
    fun getSessionHistory(): Flow<List<ShoppingSessionWithItems>>
    suspend fun startSession(storeName: String, month: String): Long
    suspend fun endSession(sessionId: Long)
    suspend fun addItem(
        sessionId: Long,
        itemName: String,
        quantity: Double,
        unit: String,
        totalPrice: Double,
        categoryId: Long
    ): Long
    suspend fun removeItem(itemId: Long)
    suspend fun searchGroceryItems(query: String): List<GroceryItemEntity>
    fun getSessionById(sessionId: Long): Flow<ShoppingSessionWithItems?>
}
