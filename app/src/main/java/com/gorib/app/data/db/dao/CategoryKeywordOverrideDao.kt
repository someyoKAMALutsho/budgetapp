package com.gorib.app.data.db.dao

import androidx.room.*
import com.gorib.app.data.db.entity.CategoryKeywordOverrideEntity

@Dao
interface CategoryKeywordOverrideDao {
    @Query("SELECT * FROM category_keyword_overrides")
    suspend fun getAllOverrides(): List<CategoryKeywordOverrideEntity>

    @Query("SELECT * FROM category_keyword_overrides WHERE keyword = :keyword LIMIT 1")
    suspend fun getOverrideForKeyword(keyword: String): CategoryKeywordOverrideEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateOverride(override: CategoryKeywordOverrideEntity)
}
