package com.gorib.app.data.repository

import com.gorib.app.data.db.dao.CategoryDao
import com.gorib.app.data.db.entity.CategoryEntity
import com.gorib.app.domain.model.Category
import com.gorib.app.domain.repository.CategoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [CategoryRepository] utilizing Room [CategoryDao].
 */
@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { list ->
            list.map { it.toDomain() }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getCategoryById(id: Long): Category? = withContext(Dispatchers.IO) {
        categoryDao.getCategoryById(id)?.toDomain()
    }

    override fun getCategoriesByType(type: String): Flow<List<Category>> {
        return categoryDao.getCategoriesByType(type).map { list ->
            list.map { it.toDomain() }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun addCategory(category: Category): Long = withContext(Dispatchers.IO) {
        categoryDao.insertCategory(category.toEntity())
    }

    override suspend fun updateCategory(category: Category) = withContext(Dispatchers.IO) {
        categoryDao.updateCategory(category.toEntity())
    }

    override suspend fun deleteCategory(category: Category) = withContext(Dispatchers.IO) {
        categoryDao.deleteCategory(category.toEntity())
    }

    override suspend fun updateBudget(id: Long, budget: Double?) = withContext(Dispatchers.IO) {
        categoryDao.updateBudget(id, budget)
    }

    // Mapper extensions
    private fun CategoryEntity.toDomain(): Category {
        return Category(id, name, iconEmoji, type, isSystem, sortOrder, monthlyBudgetRm)
    }

    private fun Category.toEntity(): CategoryEntity {
        return CategoryEntity(id, name, iconEmoji, type, isSystem, sortOrder, monthlyBudgetRm)
    }
}
