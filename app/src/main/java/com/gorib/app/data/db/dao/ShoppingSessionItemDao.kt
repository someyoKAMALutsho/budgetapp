package com.gorib.app.data.db.dao

import androidx.room.*
import com.gorib.app.data.db.entity.ShoppingSessionItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingSessionItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ShoppingSessionItemEntity): Long

    @Delete
    suspend fun deleteItem(item: ShoppingSessionItemEntity)

    @Query("DELETE FROM shopping_session_item WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("SELECT * FROM shopping_session_item WHERE session_id = :sessionId")
    fun getItemsForSessionFlow(sessionId: Long): Flow<List<ShoppingSessionItemEntity>>

    @Query("SELECT * FROM shopping_session_item WHERE session_id = :sessionId")
    suspend fun getItemsForSessionSync(sessionId: Long): List<ShoppingSessionItemEntity>

    @Query("SELECT * FROM shopping_session_item WHERE id = :id LIMIT 1")
    suspend fun getItemByIdSync(id: Long): ShoppingSessionItemEntity?
}
