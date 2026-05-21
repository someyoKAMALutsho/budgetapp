package com.gorib.app.domain.usecase

import com.gorib.app.domain.model.Transaction
import com.gorib.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Domain UseCase that retrieves all transactions, sorted chronologically.
 */
class GetTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(): Flow<List<Transaction>> {
        return transactionRepository.getAllTransactions()
    }
}
