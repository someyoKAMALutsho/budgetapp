package com.gorib.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gorib.app.data.db.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for Budget operations.
 */
@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE month_year = :monthYear")
    fun getBudgetsForMonth(monthYear: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE category_id = :categoryId AND month_year = :monthYear LIMIT 1")
    suspend fun getBudgetByCategoryForMonth(categoryId: Long, monthYear: String): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE category_id IS NULL AND month_year = :monthYear LIMIT 1")
    suspend fun getGlobalBudgetForMonth(monthYear: String): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Query("UPDATE budgets SET spent_amount = spent_amount + :amount WHERE category_id = :categoryId AND month_year = :monthYear")
    suspend fun incrementBudgetSpend(categoryId: Long, monthYear: String, amount: Double)

    @Query("UPDATE budgets SET spent_amount = spent_amount + :amount WHERE category_id IS NULL AND month_year = :monthYear")
    suspend fun incrementGlobalBudgetSpend(monthYear: String, amount: Double)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)
}
