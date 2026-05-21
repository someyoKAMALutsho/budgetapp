package com.gorib.app.domain.usecase.grocery

import com.gorib.app.domain.repository.GroceryRepository
import javax.inject.Inject

class EndShoppingSessionUseCase @Inject constructor(
    private val groceryRepository: GroceryRepository
) {
    suspend operator fun invoke(sessionId: Long) {
        groceryRepository.endSession(sessionId)
    }
}
