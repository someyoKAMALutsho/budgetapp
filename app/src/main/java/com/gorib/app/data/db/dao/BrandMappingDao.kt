package com.gorib.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gorib.app.data.db.entity.BrandMappingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BrandMappingDao {
    @Query("SELECT * FROM brand_mappings")
    fun getAllBrandMappings(): Flow<List<BrandMappingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBrandMappings(mappings: List<BrandMappingEntity>)
}
