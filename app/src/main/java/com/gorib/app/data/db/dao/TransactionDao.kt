package com.gorib.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.ColumnInfo
import com.gorib.app.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for Transaction operations.
 */
@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE category_id = :categoryId ORDER BY timestamp DESC")
    fun getTransactionsByCategory(categoryId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE category_id = :categoryId AND billing_month = :month ORDER BY timestamp DESC")
    fun getByCategory(categoryId: Long, month: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE is_ocr_processed = 1 ORDER BY timestamp DESC")
    fun getOcrProcessedTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("""
        SELECT category_id, SUM(amount) as total
        FROM transactions
        WHERE billing_month = :month
        GROUP BY category_id
    """)
    suspend fun getSpendingByCategory(month: String): List<CategoryTotal>

    @Query("""
        SELECT CAST(strftime('%d', datetime(timestamp/1000, 'unixepoch', 'localtime')) AS INTEGER) as day,
               SUM(amount) as total
        FROM transactions
        WHERE billing_month = :month
        GROUP BY day
        ORDER BY day ASC
    """)
    suspend fun getDailySpending(month: String): List<DailyTotal>

    @Query("""
        SELECT description as merchantName,
               SUM(amount) as total,
               COUNT(*) as txCount
        FROM transactions
        WHERE billing_month = :month
        GROUP BY description
        ORDER BY total DESC
        LIMIT 5
    """)
    suspend fun getTopMerchants(month: String): List<MerchantTotal>

    @Query("""
        SELECT SUM(amount) FROM transactions
        WHERE billing_month = :month
    """)
    suspend fun getTotalForMonth(month: String): Double?
}

data class CategoryTotal(
    @ColumnInfo(name = "category_id") val categoryId: Long,
    @ColumnInfo(name = "total") val total: Double
)

data class DailyTotal(
    @ColumnInfo(name = "day") val day: String,
    @ColumnInfo(name = "total") val total: Double
)

data class MerchantTotal(
    @ColumnInfo(name = "merchantName") val merchantName: String,
    @ColumnInfo(name = "total") val total: Double,
    @ColumnInfo(name = "txCount") val txCount: Int
)
