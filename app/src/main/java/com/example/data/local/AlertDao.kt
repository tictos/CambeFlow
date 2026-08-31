package com.example.data.local

import androidx.room.*
import com.example.model.PriceAlert
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {
    @Query("SELECT * FROM price_alerts ORDER BY createdAt DESC")
    fun getAllAlerts(): Flow<List<PriceAlert>>

    @Query("SELECT * FROM price_alerts ORDER BY createdAt DESC")
    suspend fun getAllAlertsOnce(): List<PriceAlert>

    @Query("SELECT * FROM price_alerts WHERE isEnabled = 1 ORDER BY createdAt DESC")
    fun getActiveAlerts(): Flow<List<PriceAlert>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: PriceAlert): Long

    @Update
    suspend fun updateAlert(alert: PriceAlert)

    @Query("UPDATE price_alerts SET isEnabled = :enabled WHERE id = :id")
    suspend fun setAlertEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM price_alerts WHERE id = :id")
    suspend fun deleteAlertById(id: Long)

    @Query("DELETE FROM price_alerts")
    suspend fun clearAllAlerts()
}
