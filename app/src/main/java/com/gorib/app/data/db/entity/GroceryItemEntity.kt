package com.gorib.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "grocery_items")
data class GroceryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "grocery_category") val groceryCategory: String,
    @ColumnInfo(name = "aliases") val aliases: String, // Comma-separated list of aliases
    @ColumnInfo(name = "default_unit") val defaultUnit: String
)
