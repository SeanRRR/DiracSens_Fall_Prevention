package com.diracsens.fallprevention.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.diracsens.fallprevention.models.BalanceReading
import com.diracsens.fallprevention.models.BloodPressureReading
import com.diracsens.fallprevention.models.BreathingRateReading
import com.diracsens.fallprevention.models.GaitReading
import com.diracsens.fallprevention.models.HeartRateReading

@Database(
    entities = [
        BloodPressureReading::class,
        HeartRateReading::class,
        BreathingRateReading::class,
        GaitReading::class,
        BalanceReading::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bloodPressureDao(): BloodPressureDao
    abstract fun heartRateDao(): HeartRateDao
    abstract fun breathingRateDao(): BreathingRateDao
    abstract fun gaitDao(): GaitDao
    abstract fun balanceDao(): BalanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "diracsens_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}