package com.gorib.app.domain.usecase

import com.gorib.app.domain.repository.UtilityRepository
import javax.inject.Inject

class MarkUtilityPaidUseCase @Inject constructor(
    private val repository: UtilityRepository
) {
    suspend operator fun invoke(groupId: Long, paid: Boolean): Result<Unit> {
        return try {
            if (paid) {
                repository.markAsPaid(groupId)
            } else {
                repository.markAsUnpaid(groupId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
