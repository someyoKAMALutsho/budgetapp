package com.gorib.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_session")
data class ShoppingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "store_name") val storeName: String,
    @ColumnInfo(name = "billing_month") val billingMonth: String,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "started_at") val startedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "completed_at") val completedAt: Long? = null,
    @ColumnInfo(name = "total_amount") val totalAmount: Double = 0.0,
    @ColumnInfo(name = "note") val note: String? = null
)
