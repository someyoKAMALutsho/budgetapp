package com.gorib.app.domain.usecase

import com.gorib.app.domain.repository.CategoryRepository
import javax.inject.Inject

/**
 * UseCase to update the budget for a specific category.
 */
class UpdateCategoryBudgetUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(categoryId: Long, budget: Double?) =
        categoryRepository.updateBudget(categoryId, budget)
}
