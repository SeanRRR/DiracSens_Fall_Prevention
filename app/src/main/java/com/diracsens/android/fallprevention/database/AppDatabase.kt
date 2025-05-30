package com.diracsens.android.fallprevention.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.diracsens.android.fallprevention.models.BalanceReading
import com.diracsens.android.fallprevention.models.BloodPressureReading
import com.diracsens.android.fallprevention.models.BreathingRateReading
import com.diracsens.android.fallprevention.models.GaitReading
import com.diracsens.android.fallprevention.models.HeartRateReading
import com.diracsens.android.fallprevention.models.ChromiumReading
import com.diracsens.android.fallprevention.models.LeadReading
import com.diracsens.android.fallprevention.models.MercuryReading
import com.diracsens.android.fallprevention.models.CadmiumReading
import com.diracsens.android.fallprevention.models.SilverReading
import com.diracsens.android.fallprevention.models.TemperatureReading

@Database(
    entities = [
        BloodPressureReading::class,
        HeartRateReading::class,
        BreathingRateReading::class,
        GaitReading::class,
        BalanceReading::class,
        ChromiumReading::class,
        LeadReading::class,
        MercuryReading::class,
        CadmiumReading::class,
        SilverReading::class,
        TemperatureReading::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bloodPressureDao(): BloodPressureDao
    abstract fun heartRateDao(): HeartRateDao
    abstract fun breathingRateDao(): BreathingRateDao
    abstract fun gaitDao(): GaitDao
    abstract fun balanceDao(): BalanceDao
    abstract fun chromiumDao(): ChromiumDao
    abstract fun leadDao(): LeadDao
    abstract fun mercuryDao(): MercuryDao
    abstract fun cadmiumDao(): CadmiumDao
    abstract fun silverDao(): SilverDao
    abstract fun temperatureDao(): TemperatureDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "diracsens_database"
                )
                .fallbackToDestructiveMigration() // This will recreate tables if version changes
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}