package com.gorib.app.domain.usecase

import com.gorib.app.domain.model.Transaction
import com.gorib.app.domain.repository.TransactionRepository
import javax.inject.Inject

class AddTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction): Result<Long> {
        if (transaction.amount <= 0.0) {
            return Result.failure(IllegalArgumentException("Transaction amount must be greater than RM 0"))
        }
        val transactionWithMonth = if (transaction.billingMonth.isEmpty()) {
            transaction.copy(billingMonth = java.time.YearMonth.now().toString())
        } else {
            transaction
        }
        return try {
            val id = repository.addTransaction(transactionWithMonth)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
