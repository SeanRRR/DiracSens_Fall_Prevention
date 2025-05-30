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
import com.diracsens.android.fallprevention.models.ChromiumReading
import com.diracsens.android.fallprevention.models.LeadReading
import com.diracsens.android.fallprevention.models.MercuryReading
import com.diracsens.android.fallprevention.models.CadmiumReading
import com.diracsens.android.fallprevention.models.SilverReading
import com.diracsens.android.fallprevention.models.TemperatureReading

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

    @Query("DELETE FROM heart_rate_readings")
    suspend fun deleteAllReadings()
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

@Dao
interface ChromiumDao {
    @Insert
    suspend fun insert(reading: ChromiumReading)

    @Query("SELECT * FROM chromium_readings ORDER BY timestamp DESC")
    fun getAllReadings(): LiveData<List<ChromiumReading>>

    @Query("SELECT * FROM chromium_readings ORDER BY timestamp DESC")
    fun getAllReadingsSync(): List<ChromiumReading>

    @Query("SELECT * FROM chromium_readings ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentReadings(limit: Int): LiveData<List<ChromiumReading>>
}

@Dao
interface LeadDao {
    @Insert
    suspend fun insert(reading: LeadReading)

    @Query("SELECT * FROM lead_readings ORDER BY timestamp DESC")
    fun getAllReadings(): LiveData<List<LeadReading>>

    @Query("SELECT * FROM lead_readings ORDER BY timestamp DESC")
    fun getAllReadingsSync(): List<LeadReading>

    @Query("SELECT * FROM lead_readings ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentReadings(limit: Int): LiveData<List<LeadReading>>
}

@Dao
interface MercuryDao {
    @Insert
    suspend fun insert(reading: MercuryReading)

    @Query("SELECT * FROM mercury_readings ORDER BY timestamp DESC")
    fun getAllReadings(): LiveData<List<MercuryReading>>

    @Query("SELECT * FROM mercury_readings ORDER BY timestamp DESC")
    fun getAllReadingsSync(): List<MercuryReading>

    @Query("SELECT * FROM mercury_readings ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentReadings(limit: Int): LiveData<List<MercuryReading>>
}

@Dao
interface CadmiumDao {
    @Insert
    suspend fun insert(reading: CadmiumReading)

    @Query("SELECT * FROM cadmium_readings ORDER BY timestamp DESC")
    fun getAllReadings(): LiveData<List<CadmiumReading>>

    @Query("SELECT * FROM cadmium_readings ORDER BY timestamp DESC")
    fun getAllReadingsSync(): List<CadmiumReading>

    @Query("SELECT * FROM cadmium_readings ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentReadings(limit: Int): LiveData<List<CadmiumReading>>
}

@Dao
interface SilverDao {
    @Insert
    suspend fun insert(reading: SilverReading)

    @Query("SELECT * FROM silver_readings ORDER BY timestamp DESC")
    fun getAllReadings(): LiveData<List<SilverReading>>

    @Query("SELECT * FROM silver_readings ORDER BY timestamp DESC")
    fun getAllReadingsSync(): List<SilverReading>

    @Query("SELECT * FROM silver_readings ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentReadings(limit: Int): LiveData<List<SilverReading>>
}

@Dao
interface TemperatureDao {
    @Insert
    suspend fun insert(reading: TemperatureReading)

    @Query("SELECT * FROM temperature_readings ORDER BY timestamp DESC")
    fun getAllReadings(): LiveData<List<TemperatureReading>>

    @Query("SELECT * FROM temperature_readings ORDER BY timestamp DESC")
    fun getAllReadingsSync(): List<TemperatureReading>

    @Query("SELECT * FROM temperature_readings ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentReadings(limit: Int): LiveData<List<TemperatureReading>>
}