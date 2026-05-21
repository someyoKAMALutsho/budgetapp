package com.gorib.app.data.db.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * A Room relationship class representing a utility bill group along with all its child line items.
 */
data class UtilityBillGroupWithItems(
    @Embedded val group: UtilityBillGroupEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "group_id"
    )
    val lineItems: List<UtilityBillLineItemEntity>
)
