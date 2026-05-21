package com.gorib.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_expense")
data class RecurringExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "amount_rm") val amountRm: Double,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    @ColumnInfo(name = "due_day") val dueDay: Int,      // 1-28
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "note") val note: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
