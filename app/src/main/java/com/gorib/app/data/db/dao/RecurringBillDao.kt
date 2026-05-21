package com.gorib.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gorib.app.data.db.entity.RecurringBillEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for Recurring Bill operations.
 */
@Dao
interface RecurringBillDao {
    @Query("SELECT * FROM recurring_bills ORDER BY due_date ASC")
    fun getAllRecurringBills(): Flow<List<RecurringBillEntity>>

    @Query("SELECT * FROM recurring_bills WHERE is_paid = 0 ORDER BY due_date ASC")
    fun getUnpaidRecurringBills(): Flow<List<RecurringBillEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringBill(recurringBill: RecurringBillEntity): Long

    @Update
    suspend fun updateRecurringBill(recurringBill: RecurringBillEntity)

    @Query("UPDATE recurring_bills SET is_paid = :isPaid WHERE id = :id")
    suspend fun updatePaidStatus(id: Long, isPaid: Boolean)

    @Delete
    suspend fun deleteRecurringBill(recurringBill: RecurringBillEntity)
}
