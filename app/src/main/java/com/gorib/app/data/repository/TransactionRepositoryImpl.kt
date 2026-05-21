package com.gorib.app.data.repository

import com.gorib.app.data.db.dao.TransactionDao
import com.gorib.app.data.db.entity.TransactionEntity
import com.gorib.app.domain.model.Transaction
import com.gorib.app.domain.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [TransactionRepository] utilizing Room [TransactionDao].
 */
@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions().map { list ->
            list.map { it.toDomain() }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getTransactionById(id: Long): Transaction? = withContext(Dispatchers.IO) {
        transactionDao.getTransactionById(id)?.toDomain()
    }

    override fun getTransactionsByCategory(categoryId: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByCategory(categoryId).map { list ->
            list.map { it.toDomain() }
        }.flowOn(Dispatchers.IO)
    }

    override fun getTransactionsByCategoryAndMonth(categoryId: Long, month: String): Flow<List<Transaction>> {
        return transactionDao.getByCategory(categoryId, month).map { list ->
            list.map { it.toDomain() }
        }.flowOn(Dispatchers.IO)
    }

    override fun getOcrProcessedTransactions(): Flow<List<Transaction>> {
        return transactionDao.getOcrProcessedTransactions().map { list ->
            list.map { it.toDomain() }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun addTransaction(transaction: Transaction): Long = withContext(Dispatchers.IO) {
        transactionDao.insertTransaction(transaction.toEntity())
    }

    override suspend fun updateTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        transactionDao.updateTransaction(transaction.toEntity())
    }

    override suspend fun deleteTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        transactionDao.deleteTransaction(transaction.toEntity())
    }

    // Mapper extensions
    private fun TransactionEntity.toDomain(): Transaction {
        return Transaction(
            id = id,
            amount = amount,
            title = title,
            description = description,
            categoryId = categoryId,
            loggedAt = timestamp,
            type = type,
            isRecurring = isRecurring,
            billingMonth = billingMonth,
            isOcrProcessed = isOcrProcessed,
            ocrRawText = ocrRawText,
            ocrStatus = ocrStatus,
            ocrConfidence = ocrConfidence,
            receiptPath = receiptPath
        )
    }

    private fun Transaction.toEntity(): TransactionEntity {
        return TransactionEntity(
            id = id,
            amount = amount,
            title = title,
            description = description,
            categoryId = categoryId,
            timestamp = loggedAt,
            type = type,
            isRecurring = isRecurring,
            billingMonth = billingMonth,
            isOcrProcessed = isOcrProcessed,
            ocrRawText = ocrRawText,
            ocrStatus = ocrStatus,
            ocrConfidence = ocrConfidence,
            receiptPath = receiptPath
        )
    }
}
