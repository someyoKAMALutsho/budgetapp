package com.gorib.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gorib.app.data.db.entity.RecurringExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringExpenseDao {
    @Query("SELECT * FROM recurring_expense WHERE is_active = 1 ORDER BY due_day ASC")
    fun getAllActive(): Flow<List<RecurringExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RecurringExpenseEntity): Long

    @Update
    suspend fun update(entity: RecurringExpenseEntity)

    @Query("UPDATE recurring_expense SET is_active = 0 WHERE id = :id")
    suspend fun deactivate(id: Long)
}
