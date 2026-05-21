package com.gorib.app.domain.usecase.grocery

import com.gorib.app.domain.repository.GroceryRepository
import javax.inject.Inject

class RemoveSessionItemUseCase @Inject constructor(
    private val groceryRepository: GroceryRepository
) {
    suspend operator fun invoke(itemId: Long) {
        groceryRepository.removeItem(itemId)
    }
}
