package com.bgy.farmmanager.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FarmDao {
    @Query("SELECT * FROM farms ORDER BY id DESC")
    fun observeAll(): Flow<List<FarmEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(farm: FarmEntity)

    @Query("DELETE FROM farms WHERE id = :id")
    suspend fun deleteById(id: Long)
}
