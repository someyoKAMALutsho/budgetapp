package com.gorib.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing an individual transaction (income or expense).
 * Linked to [CategoryEntity] via foreign keys.
 * Includes tracking fields for OCR (Option C vs Option B).
 */
@Entity(
    tableName = "transactions",
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
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "amount")
    val amount: Double, // Amount in MYR (RM)
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "description")
    val description: String? = null,
    
    @ColumnInfo(name = "category_id")
    val categoryId: Long,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long, // Epoch timestamp (milliseconds)
    
    @ColumnInfo(name = "type")
    val type: String, // "EXPENSE" or "INCOME"
    
    @ColumnInfo(name = "is_recurring")
    val isRecurring: Boolean = false,

    @ColumnInfo(name = "billing_month")
    val billingMonth: String = "",
    
    // OCR Fallback Rule Metadata Fields
    @ColumnInfo(name = "is_ocr_processed")
    val isOcrProcessed: Boolean = false,
    
    @ColumnInfo(name = "ocr_raw_text")
    val ocrRawText: String? = null,
    
    @ColumnInfo(name = "ocr_status")
    val ocrStatus: String = "NONE", // "NONE", "PARTIAL" (Option C gaps filled), "FAILED" (Option B manual)
    
    @ColumnInfo(name = "ocr_confidence")
    val ocrConfidence: Double? = null,

    @ColumnInfo(name = "receipt_path")
    val receiptPath: String? = null
)
