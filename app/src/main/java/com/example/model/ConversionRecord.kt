package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversion_history")
data class ConversionRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fromCode: String,
    val toCode: String,
    val fromAmount: Double,
    val toAmount: Double,
    val exchangeRate: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "Complété" // "Complété", "En attente", etc.
) {
    val pairText: String get() = "$fromCode/$toCode"
}
