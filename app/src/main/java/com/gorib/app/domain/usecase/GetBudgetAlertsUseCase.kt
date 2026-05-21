package com.gorib.app.domain.usecase

import com.gorib.app.data.db.dao.CategoryDao
import com.gorib.app.data.db.dao.TransactionDao
import javax.inject.Inject

data class BudgetAlert(
    val categoryId: Long,
    val categoryName: String,
    val iconEmoji: String,
    val spent: Double,
    val budget: Double,
    val percentUsed: Double,    // spent / budget * 100
    val alertLevel: AlertLevel  // WARNING (>=80%) or CRITICAL (>=100%)
)

enum class AlertLevel { WARNING, CRITICAL }

class GetBudgetAlertsUseCase @Inject constructor(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao
) {
    suspend operator fun invoke(month: String): List<BudgetAlert> {
        val totals = transactionDao.getSpendingByCategory(month)
        val categories = categoryDao.getAllCategoriesSync()
        return totals.mapNotNull { ct ->
            val cat = categories.firstOrNull { it.id == ct.categoryId }
                ?: return@mapNotNull null
            val budget = cat.monthlyBudgetRm ?: return@mapNotNull null
            val pct = ct.total / budget * 100
            if (pct < 80.0) return@mapNotNull null
            BudgetAlert(
                categoryId = ct.categoryId,
                categoryName = cat.name,
                iconEmoji = cat.iconEmoji,
                spent = ct.total,
                budget = budget,
                percentUsed = pct,
                alertLevel = if (pct >= 100.0) AlertLevel.CRITICAL
                else AlertLevel.WARNING
            )
        }.sortedByDescending { it.percentUsed }
    }
}
