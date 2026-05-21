package com.gorib.app.data.db.dao

import androidx.room.*
import com.gorib.app.data.db.entity.ShoppingSessionEntity
import com.gorib.app.data.db.entity.ShoppingSessionWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ShoppingSessionEntity): Long

    @Update
    suspend fun updateSession(session: ShoppingSessionEntity)

    @Transaction
    @Query("SELECT * FROM shopping_session WHERE status = 'ACTIVE' LIMIT 1")
    fun getActiveSessionFlow(): Flow<ShoppingSessionWithItems?>

    @Transaction
    @Query("SELECT * FROM shopping_session WHERE status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveSessionSync(): ShoppingSessionWithItems?

    @Transaction
    @Query("SELECT * FROM shopping_session ORDER BY started_at DESC")
    fun getAllSessionsFlow(): Flow<List<ShoppingSessionWithItems>>

    @Transaction
    @Query("SELECT * FROM shopping_session WHERE id = :id LIMIT 1")
    fun getSessionByIdFlow(id: Long): Flow<ShoppingSessionWithItems?>

    @Transaction
    @Query("SELECT * FROM shopping_session WHERE id = :id LIMIT 1")
    suspend fun getSessionByIdSync(id: Long): ShoppingSessionWithItems?
}
