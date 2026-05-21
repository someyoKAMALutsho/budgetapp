package com.gorib.app.domain.usecase.grocery

import com.gorib.app.domain.repository.GroceryRepository
import javax.inject.Inject

class StartShoppingSessionUseCase @Inject constructor(
    private val groceryRepository: GroceryRepository
) {
    suspend operator fun invoke(storeName: String, month: String): Long {
        return groceryRepository.startSession(storeName, month)
    }
}
