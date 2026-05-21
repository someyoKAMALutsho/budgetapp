package com.gorib.app.domain.usecase

import com.gorib.app.domain.repository.RentConfigRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class SaveRentUseCase @Inject constructor(
    private val repository: RentConfigRepository
) {
    suspend operator fun invoke(amount: Double, note: String?): Result<Long> {
        if (amount <= 0.0) {
            return Result.failure(IllegalArgumentException("Rent amount must be greater than RM 0"))
        }
        return try {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val effectiveFrom = sdf.format(Date())
            val id = repository.saveRent(amount, note, effectiveFrom)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
