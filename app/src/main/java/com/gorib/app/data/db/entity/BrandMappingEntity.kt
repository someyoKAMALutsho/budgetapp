package com.gorib.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "brand_mappings")
data class BrandMappingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "raw_text") val rawText: String,
    @ColumnInfo(name = "mapped_item_name") val mappedItemName: String,
    @ColumnInfo(name = "confidence") val confidence: Double,
    @ColumnInfo(name = "is_user_confirmed") val isUserConfirmed: Boolean = false
)
