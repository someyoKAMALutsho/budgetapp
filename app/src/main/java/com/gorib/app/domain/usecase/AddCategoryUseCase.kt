package com.gorib.app.domain.usecase

import com.gorib.app.domain.model.Category
import com.gorib.app.domain.repository.CategoryRepository
import javax.inject.Inject

/**
 * UseCase to add a custom category.
 */
class AddCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(name: String, emoji: String): Long {
        val category = Category(
            id = 0,
            name = name.trim(),
            iconEmoji = emoji,
            type = "EXPENSE",
            isSystem = false,
            sortOrder = 10,
            monthlyBudgetRm = null
        )
        return categoryRepository.addCategory(category)
    }
}
