package com.gorib.app.domain.usecase.analytics

import com.gorib.app.data.db.dao.CategoryDao
import com.gorib.app.data.db.dao.TransactionDao
import com.gorib.app.domain.model.analytics.CategoryChange
import com.gorib.app.domain.model.analytics.MonthComparisonResult
import javax.inject.Inject

class GetMonthComparisonUseCase @Inject constructor(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao
) {
    suspend operator fun invoke(
        currentMonth: String,
        previousMonth: String
    ): MonthComparisonResult {
        val currentTotals = transactionDao.getSpendingByCategory(currentMonth)
        val previousTotals = transactionDao.getSpendingByCategory(previousMonth)
        val allCategories = categoryDao.getAllCategoriesSync()

        val currentGrand = currentTotals.sumOf { it.total }
        val previousGrand = previousTotals.sumOf { it.total }
        val changePercent = if (previousGrand > 0)
            ((currentGrand - previousGrand) / previousGrand) * 100
        else 0.0

        val prevMap = previousTotals.associate { it.categoryId to it.total }

        val categoryChanges = currentTotals.mapNotNull { ct ->
            val cat = allCategories.firstOrNull { it.id == ct.categoryId }
                ?: return@mapNotNull null
            val prev = prevMap[ct.categoryId] ?: 0.0
            val pct = if (prev > 0) ((ct.total - prev) / prev) * 100 else 0.0
            CategoryChange(
                categoryName = cat.name,
                iconEmoji = cat.iconEmoji,
                currentSpent = ct.total,
                previousSpent = prev,
                changePercent = pct
            )
        }.sortedByDescending { it.currentSpent }

        return MonthComparisonResult(
            currentTotal = currentGrand,
            previousTotal = previousGrand,
            changePercent = changePercent,
            categoryChanges = categoryChanges
        )
    }
}
