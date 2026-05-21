package com.gorib.app.domain.usecase

import com.gorib.app.data.db.entity.RentConfigEntity
import com.gorib.app.domain.repository.RentConfigRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCurrentRentUseCase @Inject constructor(
    private val repository: RentConfigRepository
) {
    operator fun invoke(): Flow<RentConfigEntity?> {
        return repository.getCurrentRent()
    }
}
