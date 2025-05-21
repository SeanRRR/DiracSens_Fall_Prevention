package com.diracsens.android.fallprevention.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.diracsens.android.fallprevention.models.BalanceReading
import com.diracsens.android.fallprevention.models.BloodPressureReading
import com.diracsens.android.fallprevention.models.BreathingRateReading
import com.diracsens.android.fallprevention.models.GaitReading
import com.diracsens.android.fallprevention.models.HeartRateReading

@Dao
interface BloodPressureDao {
    @Insert
    suspend fun insert(reading: BloodPressureReading)

    @Query("SELECT * FROM blood_pressure_readings ORDER BY timestamp DESC")
    fun getAllReadings(): LiveData<List<BloodPressureReading>>

    @Query("SELECT * FROM blood_pressure_readings ORDER BY timestamp DESC")
    fun getAllReadingsSync(): List<BloodPressureReading>

    @Query("SELECT * FROM blood_pressure_readings ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentReadings(limit: Int): LiveData<List<BloodPressureReading>>
}

@Dao
interface HeartRateDao {
    @Insert
    suspend fun insert(reading: HeartRateReading)

    @Query("SELECT * FROM heart_rate_readings ORDER BY timestamp DESC")
    fun getAllReadings(): LiveData<List<HeartRateReading>>

    @Query("SELECT * FROM heart_rate_readings ORDER BY timestamp DESC")
    fun getAllReadingsSync(): List<HeartRateReading>

    @Query("SELECT * FROM heart_rate_readings ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentReadings(limit: Int): LiveData<List<HeartRateReading>>
}

@Dao
interface BreathingRateDao {
    @Insert
    suspend fun insert(reading: BreathingRateReading)

    @Query("SELECT * FROM breathing_rate_readings ORDER BY timestamp DESC")
    fun getAllReadings(): LiveData<List<BreathingRateReading>>

    @Query("SELECT * FROM breathing_rate_readings ORDER BY timestamp DESC")
    fun getAllReadingsSync(): List<BreathingRateReading>

    @Query("SELECT * FROM breathing_rate_readings ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentReadings(limit: Int): LiveData<List<BreathingRateReading>>
}

@Dao
interface GaitDao {
    @Insert
    suspend fun insert(reading: GaitReading)

    @Query("SELECT * FROM gait_readings ORDER BY timestamp DESC")
    fun getAllReadings(): LiveData<List<GaitReading>>

    @Query("SELECT * FROM gait_readings ORDER BY timestamp DESC")
    fun getAllReadingsSync(): List<GaitReading>

    @Query("SELECT * FROM gait_readings ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentReadings(limit: Int): LiveData<List<GaitReading>>
}

@Dao
interface BalanceDao {
    @Insert
    suspend fun insert(reading: BalanceReading)

    @Query("SELECT * FROM balance_readings ORDER BY timestamp DESC")
    fun getAllReadings(): LiveData<List<BalanceReading>>

    @Query("SELECT * FROM balance_readings ORDER BY timestamp DESC")
    fun getAllReadingsSync(): List<BalanceReading>

    @Query("SELECT * FROM balance_readings ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentReadings(limit: Int): LiveData<List<BalanceReading>>
}