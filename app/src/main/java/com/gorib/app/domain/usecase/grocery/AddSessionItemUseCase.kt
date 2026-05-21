package com.gorib.app.domain.usecase.grocery

import com.gorib.app.domain.repository.GroceryRepository
import javax.inject.Inject

class AddSessionItemUseCase @Inject constructor(
    private val groceryRepository: GroceryRepository
) {
    suspend operator fun invoke(
        sessionId: Long,
        itemName: String,
        quantity: Double,
        unit: String,
        totalPrice: Double,
        categoryId: Long
    ): Long {
        return groceryRepository.addItem(sessionId, itemName, quantity, unit, totalPrice, categoryId)
    }
}
