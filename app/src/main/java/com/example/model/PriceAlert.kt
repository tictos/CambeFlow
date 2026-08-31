package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AlertDirection {
    ABOVE, // Cible à la hausse
    BELOW  // Cible à la baisse
}

@Entity(tableName = "price_alerts")
data class PriceAlert(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val baseCode: String,
    val targetCode: String,
    val targetRate: Double,
    val currentRate: Double,
    val direction: AlertDirection,
    val isEnabled: Boolean = true,
    val isTriggered: Boolean = false,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val triggeredAt: Long? = null
) {
    val pairSymbol: String get() = "$baseCode/$targetCode"

    // Progress percentage towards the target (0.0 to 1.0)
    fun calculateProgress(liveRate: Double): Float {
        if (direction == AlertDirection.ABOVE) {
            if (targetRate <= 0) return 0f
            val ratio = (liveRate / targetRate).toFloat()
            return ratio.coerceIn(0.05f, 1.0f)
        } else {
            if (liveRate <= 0) return 0f
            val ratio = (targetRate / liveRate).toFloat()
            return ratio.coerceIn(0.05f, 1.0f)
        }
    }
}
