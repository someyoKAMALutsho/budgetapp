package com.gorib.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gorib.app.data.db.entity.RentConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RentConfigDao {
    @Query("SELECT * FROM rent_configs ORDER BY effective_from DESC LIMIT 1")
    fun getCurrentRent(): Flow<RentConfigEntity?>

    @Query("SELECT * FROM rent_configs ORDER BY effective_from DESC")
    fun getRentHistory(): Flow<List<RentConfigEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRentConfig(config: RentConfigEntity): Long

    @Update
    suspend fun updateRentConfig(config: RentConfigEntity)

    @Query("SELECT * FROM rent_configs WHERE id = :id LIMIT 1")
    suspend fun getRentById(id: Long): RentConfigEntity?
}
