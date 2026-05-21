package com.gorib.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "shopping_session_item",
    foreignKeys = [
        ForeignKey(
            entity = ShoppingSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ShoppingSessionItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "item_name") val itemName: String,
    @ColumnInfo(name = "quantity") val quantity: Double,
    @ColumnInfo(name = "unit") val unit: String,
    @ColumnInfo(name = "price_per_unit") val pricePerUnit: Double,
    @ColumnInfo(name = "total_price") val totalPrice: Double,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    @ColumnInfo(name = "grocery_item_id") val groceryItemId: Long? = null
)
