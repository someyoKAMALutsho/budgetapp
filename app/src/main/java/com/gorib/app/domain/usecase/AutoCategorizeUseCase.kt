package com.gorib.app.domain.usecase

import android.content.Context
import com.gorib.app.data.db.dao.CategoryDao
import com.gorib.app.data.db.dao.CategoryKeywordOverrideDao
import com.gorib.app.data.db.entity.CategoryKeywordOverrideEntity
import com.gorib.app.domain.model.CategorySuggestion
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject

/**
 * UseCase to automatically categorize transaction description based on keywords.
 */
class AutoCategorizeUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val overrideDao: CategoryKeywordOverrideDao,
    private val categoryDao: CategoryDao
) {
    suspend operator fun invoke(description: String): CategorySuggestion {
        val input = description.lowercase().trim()
        if (input.isEmpty()) {
            return CategorySuggestion(categoryId = 9L, categoryName = "Others", confidence = 0.40f)
        }

        // 1. Check learned overrides first
        val override = overrideDao.getOverrideForKeyword(input)
        if (override != null) {
            val category = categoryDao.getCategoryById(override.categoryId)
            return CategorySuggestion(
                categoryId = override.categoryId,
                categoryName = category?.name ?: "Others",
                confidence = 0.95f
            )
        }

        try {
            // 2. Load keywords from JSON asset
            val json = context.assets.open("category_keywords.json")
                .bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(json)

            // 3. Match against keywords
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val categoryName = keys.next()
                val keywords = jsonObject.getJSONArray(categoryName)
                for (i in 0 until keywords.length()) {
                    if (input.contains(keywords.getString(i).lowercase())) {
                        val category = categoryDao.getCategoryByName(categoryName)
                        return if (category != null) {
                            CategorySuggestion(category.id, category.name, 0.85f)
                        } else {
                            // Fuzzy fallback: partial match against all DB categories
                            val allCats = categoryDao.getAllCategoriesSync()
                            val fuzzy = allCats.firstOrNull { c ->
                                c.name.lowercase().contains(categoryName.lowercase()) ||
                                categoryName.lowercase().contains(c.name.lowercase())
                            }
                            if (fuzzy != null) CategorySuggestion(fuzzy.id, fuzzy.name, 0.75f)
                            else CategorySuggestion(9L, "Others", 0.40f)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 4. Default: Others
        return CategorySuggestion(categoryId = 9L, categoryName = "Others", confidence = 0.40f)
    }

    /**
     * Learns a user classification mapping for a merchant/title.
     */
    suspend fun saveOverride(title: String, categoryId: Long) {
        val query = title.trim().lowercase()
        if (query.isNotEmpty() && categoryId in 1L..10L) {
            val override = CategoryKeywordOverrideEntity(
                keyword = query,
                categoryId = categoryId
            )
            overrideDao.insertOrUpdateOverride(override)
        }
    }
}
