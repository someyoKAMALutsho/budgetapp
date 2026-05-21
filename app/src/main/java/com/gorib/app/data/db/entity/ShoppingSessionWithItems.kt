package com.gorib.app.data.db.entity

import androidx.room.Embedded
import androidx.room.Relation

data class ShoppingSessionWithItems(
    @Embedded val session: ShoppingSessionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "session_id"
    )
    val items: List<ShoppingSessionItemEntity>
)
