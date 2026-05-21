package com.gorib.app.domain.usecase.analytics

import com.gorib.app.data.db.dao.CategoryDao
import com.gorib.app.data.db.dao.TransactionDao
import com.gorib.app.domain.model.analytics.CategorySpending
import com.gorib.app.domain.model.analytics.DailySpending
import com.gorib.app.domain.model.analytics.MerchantSpending
import com.gorib.app.domain.model.analytics.MonthlySpendingSummary
import javax.inject.Inject

class GetMonthlySpendingUseCase @Inject constructor(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao
) {
    suspend operator fun invoke(month: String): MonthlySpendingSummary {
        val categoryTotals = transactionDao.getSpendingByCategory(month)
        val dailyRaw = transactionDao.getDailySpending(month)
        val merchantRaw = transactionDao.getTopMerchants(month)
        val allCategories = categoryDao.getAllCategoriesSync()

        val grandTotal = categoryTotals.sumOf { it.total }

        val byCategory = categoryTotals.mapNotNull { ct ->
            val cat = allCategories.firstOrNull { it.id == ct.categoryId }
                ?: return@mapNotNull null
            CategorySpending(
                categoryId = ct.categoryId,
                categoryName = cat.name,
                iconEmoji = cat.iconEmoji,
                totalSpent = ct.total,
                budgetLimit = cat.monthlyBudgetRm,
                percentage = if (grandTotal > 0) ct.total / grandTotal * 100 else 0.0
            )
        }.sortedByDescending { it.totalSpent }

        val dailyTotals = dailyRaw.map {
            DailySpending(day = it.day.toIntOrNull() ?: 0, total = it.total)
        }

        val topMerchants = merchantRaw.map {
            MerchantSpending(it.merchantName, it.total, it.txCount)
        }

        val totalBudget = allCategories.sumOf { it.monthlyBudgetRm ?: 0.0 }

        return MonthlySpendingSummary(
            totalSpent = grandTotal,
            totalBudget = totalBudget,
            byCategory = byCategory,
            dailyTotals = dailyTotals,
            topMerchants = topMerchants
        )
    }
}
