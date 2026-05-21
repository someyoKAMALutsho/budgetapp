package com.gorib.app.domain.usecase

import com.gorib.app.domain.repository.UtilityLineItemInput
import com.gorib.app.domain.repository.UtilityRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class SaveUtilityBillUseCase @Inject constructor(
    private val repository: UtilityRepository
) {
    suspend fun saveSingle(
        type: String, 
        customName: String?, 
        amount: Double,
        note: String?, 
        receiptPath: String?
    ): Result<Long> {
        if (amount <= 0.0) {
            return Result.failure(IllegalArgumentException("Amount must be greater than RM 0"))
        }
        return try {
            val month = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
            val id = repository.saveSingleBill(type, customName, amount, month, note, receiptPath)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveCombined(
        lineItems: List<UtilityLineItemInput>, 
        note: String?, 
        receiptPath: String?
    ): Result<Long> {
        if (lineItems.isEmpty()) {
            return Result.failure(IllegalArgumentException("At least one line item is required"))
        }
        if (lineItems.any { it.amount <= 0.0 }) {
            return Result.failure(IllegalArgumentException("All item amounts must be greater than RM 0"))
        }
        return try {
            val month = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
            val id = repository.saveCombinedBill(lineItems, month, note, receiptPath)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
