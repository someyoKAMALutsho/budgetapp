package com.gorib.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing transaction categories.
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "icon_emoji") val iconEmoji: String,
    @ColumnInfo(name = "type") val type: String = "BOTH", // "EXPENSE", "INCOME", or "BOTH"
    @ColumnInfo(name = "is_system") val isSystem: Boolean = false,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "monthly_budget_rm") val monthlyBudgetRm: Double? = null
)
