package com.gorib.app.domain.usecase

import com.gorib.app.domain.model.Transaction
import com.gorib.app.domain.repository.TransactionRepository
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

class UpdateTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction): Result<Unit> {
        if (transaction.amount <= 0.0) {
            return Result.failure(IllegalArgumentException("Transaction amount must be greater than RM 0"))
        }

        // Align billingMonth to YearMonth of transaction loggedAt
        val zoneId = ZoneId.systemDefault()
        val yearMonth = YearMonth.from(Instant.ofEpochMilli(transaction.loggedAt).atZone(zoneId))
        val updatedTransaction = transaction.copy(billingMonth = yearMonth.toString())

        return try {
            repository.updateTransaction(updatedTransaction)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
