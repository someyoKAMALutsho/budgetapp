package com.gorib.app.domain.usecase

import com.gorib.app.data.db.dao.RentConfigDao
import com.gorib.app.data.db.entity.RentConfigEntity
import com.gorib.app.data.preferences.UserPreferencesRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Domain UseCase that handles saving the initial Rent Entry during the first-launch setup wizard.
 * Sets preferences, records the rent configuration separately from category transactions.
 */
class SaveRentSetupUseCase @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val rentConfigDao: RentConfigDao
) {
    suspend operator fun invoke(rentAmount: Double, payNow: Boolean): Result<Unit> {
        if (rentAmount <= 0.0) {
            return Result.failure(IllegalArgumentException("Rent amount must be greater than RM 0"))
        }

        return try {
            // 1. Save rent and onboarding completion to preferences
            preferencesRepository.setMonthlyRent(rentAmount)
            preferencesRepository.setFirstLaunchCompleted()

            // 2. Save source of truth RentConfigEntity to database separately
            val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val effectiveFrom = sdf.format(Date())
            
            val rentConfig = RentConfigEntity(
                amount = rentAmount,
                effectiveFrom = effectiveFrom,
                paymentStatus = "UNPAID",
                paidOnDate = null,
                note = "Initial rent configured via setup wizard"
            )
            rentConfigDao.insertOrUpdateRentConfig(rentConfig)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
