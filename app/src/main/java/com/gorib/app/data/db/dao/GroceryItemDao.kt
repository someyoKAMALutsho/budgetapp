package com.gorib.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gorib.app.data.db.entity.GroceryItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroceryItemDao {
    @Query("SELECT * FROM grocery_items")
    fun getAllGroceryItems(): Flow<List<GroceryItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroceryItems(items: List<GroceryItemEntity>)

    @Query("SELECT * FROM grocery_items WHERE name LIKE :likeQuery OR aliases LIKE :likeQuery")
    suspend fun searchGroceryItemsSync(likeQuery: String): List<GroceryItemEntity>
}
