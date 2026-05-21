package com.gorib.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a utility bill group, which can hold single or combined utility line items.
 */
@Entity(tableName = "utility_bill_group")
data class UtilityBillGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "billing_month") val billingMonth: String, // YYYY-MM
    @ColumnInfo(name = "payment_status") val paymentStatus: String = "UNPAID",
    @ColumnInfo(name = "paid_on_date") val paidOnDate: String? = null,
    @ColumnInfo(name = "is_combined") val isCombined: Boolean = false,
    @ColumnInfo(name = "total_amount") val totalAmount: Double,
    @ColumnInfo(name = "note") val note: String? = null,
    @ColumnInfo(name = "receipt_path") val receiptPath: String? = null
)
