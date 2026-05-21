package com.gorib.app.domain.model.analytics

data class MerchantSpending(
    val merchantName: String,
    val totalSpent: Double,
    val transactionCount: Int
)
