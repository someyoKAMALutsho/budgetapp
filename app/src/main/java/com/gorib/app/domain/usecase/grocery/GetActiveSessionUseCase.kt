package com.gorib.app.domain.usecase.grocery

import com.gorib.app.data.db.entity.ShoppingSessionWithItems
import com.gorib.app.domain.repository.GroceryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetActiveSessionUseCase @Inject constructor(
    private val groceryRepository: GroceryRepository
) {
    operator fun invoke(): Flow<ShoppingSessionWithItems?> {
        return groceryRepository.getActiveSession()
    }
}
