package com.gorib.app.domain.usecase

import com.gorib.app.domain.repository.RentConfigRepository
import javax.inject.Inject

class MarkRentPaidUseCase @Inject constructor(
    private val repository: RentConfigRepository
) {
    suspend operator fun invoke(rentId: Long, paid: Boolean): Result<Unit> {
        return try {
            if (paid) {
                repository.markAsPaid(rentId)
            } else {
                repository.markAsUnpaid(rentId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
