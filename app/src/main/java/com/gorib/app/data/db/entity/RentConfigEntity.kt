package com.gorib.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing rent config. Kept separate from category expenses.
 */
@Entity(tableName = "rent_configs")
data class RentConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "amount_rm") val amount: Double,
    @ColumnInfo(name = "effective_from") val effectiveFrom: String, // YYYY-MM format
    @ColumnInfo(name = "payment_status") val paymentStatus: String = "UNPAID",
    @ColumnInfo(name = "paid_on_date") val paidOnDate: String? = null,
    @ColumnInfo(name = "note") val note: String? = null
)
