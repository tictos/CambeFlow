package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.model.AlertDirection
import com.example.model.ConversionRecord
import com.example.model.PriceAlert

class Converters {
    @TypeConverter
    fun fromAlertDirection(value: AlertDirection): String = value.name

    @TypeConverter
    fun toAlertDirection(value: String): AlertDirection = try {
        AlertDirection.valueOf(value)
    } catch (e: Exception) {
        AlertDirection.ABOVE
    }
}

@Database(
    entities = [PriceAlert::class, ConversionRecord::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TradeFlowDatabase : RoomDatabase() {
    abstract fun alertDao(): AlertDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: TradeFlowDatabase? = null

        fun getDatabase(context: Context): TradeFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TradeFlowDatabase::class.java,
                    "tradeflow_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
