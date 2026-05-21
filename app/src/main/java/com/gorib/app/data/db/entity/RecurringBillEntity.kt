package com.gorib.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing recurring bills scheduling details.
 * Amount has been removed to avoid redundancy (stored in RentConfigEntity).
 */
@Entity(
    tableName = "recurring_bills",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("category_id")
    ]
)
data class RecurringBillEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "due_date")
    val dueDate: Long, // Next due date (Epoch timestamp)
    
    @ColumnInfo(name = "category_id")
    val categoryId: Long,
    
    @ColumnInfo(name = "recurrence_period")
    val recurrencePeriod: String = "MONTHLY", // "WEEKLY", "MONTHLY", "YEARLY"
    
    @ColumnInfo(name = "is_paid")
    val isPaid: Boolean = false,
    
    @ColumnInfo(name = "auto_pay")
    val autoPay: Boolean = false
)
