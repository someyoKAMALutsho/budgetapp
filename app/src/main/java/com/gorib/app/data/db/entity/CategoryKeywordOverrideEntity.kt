package com.gorib.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing learned category keyword overrides from manual user corrections.
 */
@Entity(tableName = "category_keyword_overrides")
data class CategoryKeywordOverrideEntity(
    @PrimaryKey val keyword: String, // Lowercase merchant/keyword
    @ColumnInfo(name = "category_id") val categoryId: Long
)
