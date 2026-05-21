package com.gorib.app.domain.usecase.grocery

import com.gorib.app.data.db.entity.GroceryItemEntity
import com.gorib.app.domain.repository.GroceryRepository
import javax.inject.Inject

class SearchGroceryItemsUseCase @Inject constructor(
    private val groceryRepository: GroceryRepository
) {
    suspend operator fun invoke(query: String): List<GroceryItemEntity> {
        return groceryRepository.searchGroceryItems(query)
    }
}
