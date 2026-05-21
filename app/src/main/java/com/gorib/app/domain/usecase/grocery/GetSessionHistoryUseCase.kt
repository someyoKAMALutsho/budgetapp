package com.gorib.app.domain.usecase.grocery

import com.gorib.app.data.db.entity.ShoppingSessionWithItems
import com.gorib.app.domain.repository.GroceryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSessionHistoryUseCase @Inject constructor(
    private val groceryRepository: GroceryRepository
) {
    operator fun invoke(): Flow<List<ShoppingSessionWithItems>> {
        return groceryRepository.getSessionHistory()
    }
}
