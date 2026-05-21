package com.gorib.app.domain.usecase.recurring

import com.gorib.app.domain.repository.RecurringExpenseRepository
import javax.inject.Inject

class DeactivateRecurringUseCase @Inject constructor(
    private val repository: RecurringExpenseRepository
) {
    suspend operator fun invoke(id: Long) {
        repository.deactivate(id)
    }
}
