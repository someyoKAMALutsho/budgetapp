package com.gorib.app.domain.usecase

import com.gorib.app.domain.model.Category
import com.gorib.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UseCase to retrieve all system and custom categories.
 */
class GetAllCategoriesUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    operator fun invoke(): Flow<List<Category>> =
        categoryRepository.getAllCategories()
}
