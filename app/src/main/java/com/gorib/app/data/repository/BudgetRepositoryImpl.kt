package com.gorib.app.data.repository

import com.gorib.app.data.db.dao.BudgetDao
import com.gorib.app.data.db.entity.BudgetEntity
import com.gorib.app.domain.model.Budget
import com.gorib.app.domain.repository.BudgetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [BudgetRepository] utilizing Room [BudgetDao].
 */
@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao
) : BudgetRepository {

    override fun getBudgetsForMonth(monthYear: String): Flow<List<Budget>> {
        return budgetDao.getBudgetsForMonth(monthYear).map { list ->
            list.map { it.toDomain() }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getBudgetByCategoryForMonth(categoryId: Long, monthYear: String): Budget? =
        withContext(Dispatchers.IO) {
            budgetDao.getBudgetByCategoryForMonth(categoryId, monthYear)?.toDomain()
        }

    override suspend fun getGlobalBudgetForMonth(monthYear: String): Budget? =
        withContext(Dispatchers.IO) {
            budgetDao.getGlobalBudgetForMonth(monthYear)?.toDomain()
        }

    override suspend fun addBudget(budget: Budget): Long = withContext(Dispatchers.IO) {
        budgetDao.insertBudget(budget.toEntity())
    }

    override suspend fun updateBudget(budget: Budget) = withContext(Dispatchers.IO) {
        budgetDao.updateBudget(budget.toEntity())
    }

    override suspend fun incrementBudgetSpend(categoryId: Long, monthYear: String, amount: Double) =
        withContext(Dispatchers.IO) {
            budgetDao.incrementBudgetSpend(categoryId, monthYear, amount)
        }

    override suspend fun incrementGlobalBudgetSpend(monthYear: String, amount: Double) =
        withContext(Dispatchers.IO) {
            budgetDao.incrementGlobalBudgetSpend(monthYear, amount)
        }

    override suspend fun deleteBudget(budget: Budget) = withContext(Dispatchers.IO) {
        budgetDao.deleteBudget(budget.toEntity())
    }

    // Mapper extensions
    private fun BudgetEntity.toDomain(): Budget {
        return Budget(id, categoryId, limitAmount, spentAmount, period, monthYear)
    }

    private fun Budget.toEntity(): BudgetEntity {
        return BudgetEntity(id, categoryId, limitAmount, spentAmount, period, monthYear)
    }
}
