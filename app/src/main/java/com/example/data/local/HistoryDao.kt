package com.example.data.local

import androidx.room.*
import com.example.model.ConversionRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM conversion_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<ConversionRecord>>

    @Query("SELECT * FROM conversion_history WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    fun getHistorySince(sinceTimestamp: Long): Flow<List<ConversionRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ConversionRecord): Long

    @Delete
    suspend fun delete(record: ConversionRecord)

    @Query("DELETE FROM conversion_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM conversion_history")
    suspend fun clearAll()
}
