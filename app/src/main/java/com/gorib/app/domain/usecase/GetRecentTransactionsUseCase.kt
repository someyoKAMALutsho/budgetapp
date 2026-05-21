package com.gorib.app.domain.usecase

import com.gorib.app.domain.model.Transaction
import com.gorib.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetRecentTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(limit: Int = 10): Flow<List<Transaction>> {
        return repository.getAllTransactions().map { list ->
            list.take(limit)
        }
    }
}
