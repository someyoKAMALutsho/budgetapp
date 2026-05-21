package com.gorib.app.domain.repository

import com.gorib.app.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Domain-level Repository interface for Transaction operations.
 */
interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    suspend fun getTransactionById(id: Long): Transaction?
    fun getTransactionsByCategory(categoryId: Long): Flow<List<Transaction>>
    fun getTransactionsByCategoryAndMonth(categoryId: Long, month: String): Flow<List<Transaction>>
    fun getOcrProcessedTransactions(): Flow<List<Transaction>>
    suspend fun addTransaction(transaction: Transaction): Long
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)
}
