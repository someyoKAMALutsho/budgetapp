package com.gorib.app.data.db.dao

import androidx.room.*
import com.gorib.app.data.db.entity.UtilityBillGroupEntity
import com.gorib.app.data.db.entity.UtilityBillLineItemEntity
import com.gorib.app.data.db.entity.UtilityBillGroupWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface UtilityBillGroupDao {
    @Transaction
    @Query("SELECT * FROM utility_bill_group WHERE billing_month = :month")
    fun getGroupsWithItemsForMonth(month: String): Flow<List<UtilityBillGroupWithItems>>

    @Transaction
    @Query("SELECT * FROM utility_bill_group")
    fun getAllGroupsWithItems(): Flow<List<UtilityBillGroupWithItems>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUtilityBillGroup(group: UtilityBillGroupEntity): Long

    @Update
    suspend fun updateUtilityBillGroup(group: UtilityBillGroupEntity)

    @Delete
    suspend fun deleteUtilityBillGroup(group: UtilityBillGroupEntity)

    @Query("SELECT * FROM utility_bill_group WHERE id = :id LIMIT 1")
    suspend fun getGroupById(id: Long): UtilityBillGroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLineItems(items: List<UtilityBillLineItemEntity>)

    @Query("DELETE FROM utility_bill_line_item WHERE group_id = :groupId")
    suspend fun deleteLineItemsByGroupId(groupId: Long)

    @Query("SELECT COUNT(*) FROM utility_bill_group WHERE payment_status = 'UNPAID'")
    fun getUnpaidCount(): Flow<Int>
}
