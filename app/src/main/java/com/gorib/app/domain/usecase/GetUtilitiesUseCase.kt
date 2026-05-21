package com.gorib.app.domain.usecase

import com.gorib.app.data.db.entity.UtilityBillGroupWithItems
import com.gorib.app.domain.repository.UtilityRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUtilitiesUseCase @Inject constructor(
    private val repository: UtilityRepository
) {
    operator fun invoke(month: String): Flow<List<UtilityBillGroupWithItems>> {
        return repository.getUtilitiesForMonth(month)
    }

    fun getAll(): Flow<List<UtilityBillGroupWithItems>> {
        return repository.getAllUtilities()
    }
}
