package com.gorib.app.domain.usecase.recurring

import com.gorib.app.domain.repository.RecurringExpenseRepository
import javax.inject.Inject

class AddRecurringExpenseUseCase @Inject constructor(
    private val repository: RecurringExpenseRepository
) {
    suspend operator fun invoke(
        name: String,
        amount: Double,
        categoryId: Long,
        dueDay: Int,
        note: String?
    ): Long {
        require(name.isNotBlank()) { "Name cannot be blank" }
        require(amount > 0) { "Amount must be greater than zero" }
        require(dueDay in 1..28) { "Due day must be between 1 and 28" }
        return repository.addRecurring(name, amount, categoryId, dueDay, note)
    }
}
