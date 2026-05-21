package com.gorib.app.domain.usecase.recurring

import com.gorib.app.domain.model.RecurringExpense
import com.gorib.app.domain.repository.RecurringExpenseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRecurringExpensesUseCase @Inject constructor(
    private val repository: RecurringExpenseRepository
) {
    operator fun invoke(): Flow<List<RecurringExpense>> {
        return repository.getActiveRecurring()
    }
}
