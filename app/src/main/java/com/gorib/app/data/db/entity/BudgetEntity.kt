package com.gorib.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing category budgets or global budget limits.
 * Linked to [CategoryEntity] optionally.
 */
@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("category_id")
    ]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "category_id")
    val categoryId: Long? = null, // Null indicates overall total budget
    
    @ColumnInfo(name = "limit_amount")
    val limitAmount: Double, // Limit in MYR (RM)
    
    @ColumnInfo(name = "spent_amount")
    val spentAmount: Double = 0.0, // Spent amount in MYR (RM)
    
    @ColumnInfo(name = "period")
    val period: String = "MONTHLY", // "MONTHLY", "WEEKLY"
    
    @ColumnInfo(name = "month_year")
    val monthYear: String // Format: "YYYY-MM"
)
