                             package com.diracsens.android.fallprevention.repositories

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import com.diracsens.android.fallprevention.database.AppDatabase
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
import com.diracsens.android.fallprevention.services.BluetoothService
import com.opencsv.CSVWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HealthDataRepository private constructor(private val database: AppDatabase) {

    companion object {
        @Volatile
        private var INSTANCE: HealthDataRepository? = null

        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = HealthDataRepository(AppDatabase.getInstance(context))
            }
        }

        fun getInstance(): HealthDataRepository {
            return INSTANCE ?: throw IllegalStateException("Repository must be initialized")
        }
    }

    // Blood Pressure
    suspend fun insertBloodPressure(reading: BloodPressureReading) {
        database.bloodPressureDao().insert(reading)
    }

    fun getAllBloodPressureReadings(): LiveData<List<BloodPressureReading>> {
        return database.bloodPressureDao().getAllReadings()
    }

    fun getRecentBloodPressureReadings(limit: Int): LiveData<List<BloodPressureReading>> {
        return database.bloodPressureDao().getRecentReadings(limit)
    }

    // Heart Rate
    suspend fun insertHeartRate(reading: HeartRateReading) {
        database.heartRateDao().insert(reading)
    }

    fun getAllHeartRateReadings(): LiveData<List<HeartRateReading>> {
        return database.heartRateDao().getAllReadings()
    }

    fun getRecentHeartRateReadings(limit: Int): LiveData<List<HeartRateReading>> {
        return database.heartRateDao().getRecentReadings(limit)
    }

    suspend fun deleteAllHeartRateReadings() {
        database.heartRateDao().deleteAllReadings()
    }

    // Breathing Rate
    suspend fun insertBreathingRate(reading: BreathingRateReading) {
        database.breathingRateDao().insert(reading)
    }

    fun getAllBreathingRateReadings(): LiveData<List<BreathingRateReading>> {
        return database.breathingRateDao().getAllReadings()
    }

    fun getRecentBreathingRateReadings(limit: Int): LiveData<List<BreathingRateReading>> {
        return database.breathingRateDao().getRecentReadings(limit)
    }

    // Gait
    suspend fun insertGait(reading: GaitReading) {
        database.gaitDao().insert(reading)
    }

    fun getAllGaitReadings(): LiveData<List<GaitReading>> {
        return database.gaitDao().getAllReadings()
    }

    fun getRecentGaitReadings(limit: Int): LiveData<List<GaitReading>> {
        return database.gaitDao().getRecentReadings(limit)
    }

    // Balance
    suspend fun insertBalance(reading: BalanceReading) {
        database.balanceDao().insert(reading)
    }

    fun getAllBalanceReadings(): LiveData<List<BalanceReading>> {
        return database.balanceDao().getAllReadings()
    }

    fun getRecentBalanceReadings(limit: Int): LiveData<List<BalanceReading>> {
        return database.balanceDao().getRecentReadings(limit)
    }

    // Chromium
    suspend fun insertChromiumReading(reading: ChromiumReading) {
        database.chromiumDao().insert(reading)
    }

    fun getAllChromiumReadings(): LiveData<List<ChromiumReading>> {
        return database.chromiumDao().getAllReadings()
    }

    fun getRecentChromiumReadings(limit: Int): LiveData<List<ChromiumReading>> {
        return database.chromiumDao().getRecentReadings(limit)
    }

    // Lead
    suspend fun insertLeadReading(reading: LeadReading) {
        database.leadDao().insert(reading)
    }

    fun getAllLeadReadings(): LiveData<List<LeadReading>> {
        return database.leadDao().getAllReadings()
    }

    fun getRecentLeadReadings(limit: Int): LiveData<List<LeadReading>> {
        return database.leadDao().getRecentReadings(limit)
    }

    // Mercury
    suspend fun insertMercuryReading(reading: MercuryReading) {
        database.mercuryDao().insert(reading)
    }

    fun getAllMercuryReadings(): LiveData<List<MercuryReading>> {
        return database.mercuryDao().getAllReadings()
    }

    fun getRecentMercuryReadings(limit: Int): LiveData<List<MercuryReading>> {
        return database.mercuryDao().getRecentReadings(limit)
    }

    // Cadmium
    suspend fun insertCadmiumReading(reading: CadmiumReading) {
        database.cadmiumDao().insert(reading)
    }

    fun getAllCadmiumReadings(): LiveData<List<CadmiumReading>> {
        return database.cadmiumDao().getAllReadings()
    }

    fun getRecentCadmiumReadings(limit: Int): LiveData<List<CadmiumReading>> {
        return database.cadmiumDao().getRecentReadings(limit)
    }

    // Silver
    suspend fun insertSilverReading(reading: SilverReading) {
        database.silverDao().insert(reading)
    }

    fun getAllSilverReadings(): LiveData<List<SilverReading>> {
        return database.silverDao().getAllReadings()
    }

    fun getRecentSilverReadings(limit: Int): LiveData<List<SilverReading>> {
        return database.silverDao().getRecentReadings(limit)
    }

    // Temperature
    suspend fun insertTemperatureReading(reading: TemperatureReading) {
        database.temperatureDao().insert(reading)
    }

    fun getAllTemperatureReadings(): LiveData<List<TemperatureReading>> {
        return database.temperatureDao().getAllReadings()
    }

    fun getRecentTemperatureReadings(limit: Int): LiveData<List<TemperatureReading>> {
        return database.temperatureDao().getRecentReadings(limit)
    }

    // Export data to CSV
    suspend fun exportDataToCSV(context: Context, dataType: String): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                // Get all readings to determine date range
                val readings = when (dataType) {
                    BluetoothService.DATA_TYPE_HEART_RATE -> database.heartRateDao().getAllReadingsSync()
                    BluetoothService.DATA_TYPE_BLOOD_PRESSURE -> database.bloodPressureDao().getAllReadingsSync()
                    BluetoothService.DATA_TYPE_BREATHING_RATE -> database.breathingRateDao().getAllReadingsSync()
                    BluetoothService.DATA_TYPE_BODY_BALANCE -> database.balanceDao().getAllReadingsSync()
                    BluetoothService.DATA_TYPE_GAIT -> database.gaitDao().getAllReadingsSync()
                    BluetoothService.DATA_TYPE_CHROMIUM -> database.chromiumDao().getAllReadingsSync()
                    BluetoothService.DATA_TYPE_LEAD -> database.leadDao().getAllReadingsSync()
                    BluetoothService.DATA_TYPE_MERCURY -> database.mercuryDao().getAllReadingsSync()
                    BluetoothService.DATA_TYPE_CADMIUM -> database.cadmiumDao().getAllReadingsSync()
                    BluetoothService.DATA_TYPE_SILVER -> database.silverDao().getAllReadingsSync()
                    BluetoothService.DATA_TYPE_TEMPERATURE -> database.temperatureDao().getAllReadingsSync()
                    else -> return@withContext null
                }

                if (readings.isEmpty()) {
                    Log.w("HealthDataRepository", "No data available to export for type: $dataType")
                    return@withContext null
                }

                // Get date range for filename
                val timestamps = when (dataType) {
                    BluetoothService.DATA_TYPE_HEART_RATE -> (readings as List<HeartRateReading>).map { it.timestamp }
                    BluetoothService.DATA_TYPE_BLOOD_PRESSURE -> (readings as List<BloodPressureReading>).map { it.timestamp }
                    BluetoothService.DATA_TYPE_BREATHING_RATE -> (readings as List<BreathingRateReading>).map { it.timestamp }
                    BluetoothService.DATA_TYPE_BODY_BALANCE -> (readings as List<BalanceReading>).map { it.timestamp }
                    BluetoothService.DATA_TYPE_GAIT -> (readings as List<GaitReading>).map { it.timestamp }
                    BluetoothService.DATA_TYPE_CHROMIUM -> (readings as List<ChromiumReading>).map { it.timestamp }
                    BluetoothService.DATA_TYPE_LEAD -> (readings as List<LeadReading>).map { it.timestamp }
                    BluetoothService.DATA_TYPE_MERCURY -> (readings as List<MercuryReading>).map { it.timestamp }
                    BluetoothService.DATA_TYPE_CADMIUM -> (readings as List<CadmiumReading>).map { it.timestamp }
                    BluetoothService.DATA_TYPE_SILVER -> (readings as List<SilverReading>).map { it.timestamp }
                    BluetoothService.DATA_TYPE_TEMPERATURE -> (readings as List<TemperatureReading>).map { it.timestamp }
                    else -> return@withContext null
                }

                val minTimestamp = timestamps.minOfOrNull { it } ?: 0L
                val maxTimestamp = timestamps.maxOfOrNull { it } ?: 0L
                
                val startDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(minTimestamp))
                val endDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(maxTimestamp))
                val exportTime = SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date())
                
                // Create filename with date range
                val fileName = "${dataType}_${startDate}_to_${endDate}_${exportTime}.csv"

                // Use app's specific directory
                val file = File(context.getExternalFilesDir(null), fileName)
                if (file.exists() && !file.delete()) {
                    Log.e("HealthDataRepository", "Failed to delete existing file")
                    return@withContext null
                }

                FileWriter(file).use { writer ->
                    val csvWriter = CSVWriter(writer)

                    // Write metadata header
                    csvWriter.writeNext(arrayOf("Export Date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())))
                    csvWriter.writeNext(arrayOf("Data Type", dataType))
                    csvWriter.writeNext(arrayOf("Date Range", "$startDate to $endDate"))
                    csvWriter.writeNext(arrayOf("Number of Records", readings.size.toString()))
                    csvWriter.writeNext(arrayOf()) // Empty line for readability

                    when (dataType) {
                        BluetoothService.DATA_TYPE_HEART_RATE -> {
                            // Write header
                            csvWriter.writeNext(arrayOf("Timestamp", "Heart Rate (BPM)"))

                            // Write data
                            val heartRateReadings = readings as List<HeartRateReading>
                            for (reading in heartRateReadings) {
                                val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                    .format(Date(reading.timestamp))
                                csvWriter.writeNext(arrayOf(
                                    date,
                                    reading.heartRate.toString()
                                ))
                            }
                        }
                        BluetoothService.DATA_TYPE_BLOOD_PRESSURE -> {
                            // Write header
                            csvWriter.writeNext(arrayOf("Timestamp", "Systolic", "Diastolic"))

                            // Write data
                            val bloodPressureReadings = readings as List<BloodPressureReading>
                            for (reading in bloodPressureReadings) {
                                val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                    .format(Date(reading.timestamp))
                                csvWriter.writeNext(arrayOf(
                                    date,
                                    reading.systolic.toString(),
                                    reading.diastolic.toString()
                                ))
                            }
                        }
                        BluetoothService.DATA_TYPE_BREATHING_RATE -> {
                            // Write header
                            csvWriter.writeNext(arrayOf("Timestamp", "Breathing Rate"))

                            // Write data
                            val breathingRateReadings = readings as List<BreathingRateReading>
                            for (reading in breathingRateReadings) {
                                val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                    .format(Date(reading.timestamp))
                                csvWriter.writeNext(arrayOf(
                                    date,
                                    reading.breathingRate.toString()
                                ))
                            }
                        }
                        BluetoothService.DATA_TYPE_BODY_BALANCE -> {
                            // Write header
                            csvWriter.writeNext(arrayOf("Timestamp", "Sway Area", "Sway Velocity", "AP Sway", "ML Sway"))

                            // Write data
                            val balanceReadings = readings as List<BalanceReading>
                            for (reading in balanceReadings) {
                                val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                    .format(Date(reading.timestamp))
                                csvWriter.writeNext(arrayOf(
                                    date,
                                    reading.swayArea.toString(),
                                    reading.swayVelocity.toString(),
                                    reading.anteriorPosteriorSway.toString(),
                                    reading.medialLateralSway.toString()
                                ))
                            }
                        }
                        BluetoothService.DATA_TYPE_GAIT -> {
                            // Write header
                            csvWriter.writeNext(arrayOf("Timestamp", "Walking Speed", "Step Length", "Step Length Variability", "Lateral Sway"))

                            // Write data
                            val gaitReadings = readings as List<GaitReading>
                            for (reading in gaitReadings) {
                                val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                    .format(Date(reading.timestamp))
                                csvWriter.writeNext(arrayOf(
                                    date,
                                    reading.walkingSpeed.toString(),
                                    reading.stepLength.toString(),
                                    reading.stepLengthVariability.toString(),
                                    reading.lateralSway.toString()
                                ))
                            }
                        }
                        BluetoothService.DATA_TYPE_CHROMIUM -> {
                            csvWriter.writeNext(arrayOf("Timestamp", "Chromium Value"))
                            val chromiumReadings = readings as List<ChromiumReading>
                            for (reading in chromiumReadings) {
                                val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                    .format(Date(reading.timestamp))
                                csvWriter.writeNext(arrayOf(date, reading.value.toString()))
                            }
                        }
                        BluetoothService.DATA_TYPE_LEAD -> {
                            csvWriter.writeNext(arrayOf("Timestamp", "Lead Value"))
                            val leadReadings = readings as List<LeadReading>
                            for (reading in leadReadings) {
                                val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                    .format(Date(reading.timestamp))
                                csvWriter.writeNext(arrayOf(date, reading.value.toString()))
                            }
                        }
                        BluetoothService.DATA_TYPE_MERCURY -> {
                            csvWriter.writeNext(arrayOf("Timestamp", "Mercury Value"))
                            val mercuryReadings = readings as List<MercuryReading>
                            for (reading in mercuryReadings) {
                                val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                    .format(Date(reading.timestamp))
                                csvWriter.writeNext(arrayOf(date, reading.value.toString()))
                            }
                        }
                        BluetoothService.DATA_TYPE_CADMIUM -> {
                            csvWriter.writeNext(arrayOf("Timestamp", "Cadmium Value"))
                            val cadmiumReadings = readings as List<CadmiumReading>
                            for (reading in cadmiumReadings) {
                                val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                    .format(Date(reading.timestamp))
                                csvWriter.writeNext(arrayOf(date, reading.value.toString()))
                            }
                        }
                        BluetoothService.DATA_TYPE_SILVER -> {
                            csvWriter.writeNext(arrayOf("Timestamp", "Silver Value"))
                            val silverReadings = readings as List<SilverReading>
                            for (reading in silverReadings) {
                                val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                    .format(Date(reading.timestamp))
                                csvWriter.writeNext(arrayOf(date, reading.value.toString()))
                            }
                        }
                        BluetoothService.DATA_TYPE_TEMPERATURE -> {
                            csvWriter.writeNext(arrayOf("Timestamp", "Temperature Value"))
                            val temperatureReadings = readings as List<TemperatureReading>
                            for (reading in temperatureReadings) {
                                val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                    .format(Date(reading.timestamp))
                                csvWriter.writeNext(arrayOf(date, reading.value.toString()))
                            }
                        }
                    }
                }

                // Notify media scanner to make file visible in gallery
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(file.toString()),
                    null,
                    null
                )

                // Return the file URI
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } catch (e: Exception) {
                Log.e("HealthDataRepository", "Error exporting data: ${e.message}", e)
                null
            }
        }
    }
}