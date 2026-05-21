package com.gorib.app.domain.usecase

import com.gorib.app.domain.model.Transaction
import com.gorib.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class GetTransactionsByMonthUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(month: String): Flow<List<Transaction>> {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        return repository.getAllTransactions().map { list ->
            list.filter { item ->
                try {
                    sdf.format(Date(item.loggedAt)) == month
                } catch (e: Exception) {
                    false
                }
            }
        }
    }
}
