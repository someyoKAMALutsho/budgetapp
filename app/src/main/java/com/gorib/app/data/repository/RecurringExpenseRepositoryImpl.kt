package com.gorib.app.data.repository

import com.gorib.app.data.db.dao.CategoryDao
import com.gorib.app.data.db.dao.RecurringExpenseDao
import com.gorib.app.data.db.entity.RecurringExpenseEntity
import com.gorib.app.domain.model.RecurringExpense
import com.gorib.app.domain.repository.RecurringExpenseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringExpenseRepositoryImpl @Inject constructor(
    private val recurringExpenseDao: RecurringExpenseDao,
    private val categoryDao: CategoryDao
) : RecurringExpenseRepository {

    override fun getActiveRecurring(): Flow<List<RecurringExpense>> {
        return combine(
            recurringExpenseDao.getAllActive(),
            categoryDao.getAllCategories()
        ) { activeEntities, categories ->
            activeEntities.map { entity ->
                val cat = categories.find { it.id == entity.categoryId }
                RecurringExpense(
                    id = entity.id,
                    name = entity.name,
                    amountRm = entity.amountRm,
                    categoryId = entity.categoryId,
                    categoryName = cat?.name ?: "General",
                    iconEmoji = cat?.iconEmoji ?: "✏️",
                    dueDay = entity.dueDay,
                    isActive = entity.isActive,
                    note = entity.note
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun addRecurring(
        name: String,
        amount: Double,
        categoryId: Long,
        dueDay: Int,
        note: String?
    ): Long = withContext(Dispatchers.IO) {
        val entity = RecurringExpenseEntity(
            name = name,
            amountRm = amount,
            categoryId = categoryId,
            dueDay = dueDay,
            note = note,
            isActive = true
        )
        recurringExpenseDao.insert(entity)
    }

    override suspend fun updateRecurring(expense: RecurringExpense) = withContext(Dispatchers.IO) {
        val entity = RecurringExpenseEntity(
            id = expense.id,
            name = expense.name,
            amountRm = expense.amountRm,
            categoryId = expense.categoryId,
            dueDay = expense.dueDay,
            isActive = expense.isActive,
            note = expense.note
        )
        recurringExpenseDao.update(entity)
    }

    override suspend fun deactivate(id: Long) = withContext(Dispatchers.IO) {
        recurringExpenseDao.deactivate(id)
    }
}
