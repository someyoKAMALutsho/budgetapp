package com.gorib.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Entity representing an individual utility line item linked to a parent [UtilityBillGroupEntity].
 */
@Entity(
    tableName = "utility_bill_line_item",
    foreignKeys = [
        ForeignKey(
            entity = UtilityBillGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["group_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UtilityBillLineItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "group_id", index = true) val groupId: Long,
    @ColumnInfo(name = "type") val type: String, // Electricity, Water, Internet, Gas, Waste, Custom
    @ColumnInfo(name = "custom_name") val customName: String? = null,
    @ColumnInfo(name = "amount") val amount: Double
)
