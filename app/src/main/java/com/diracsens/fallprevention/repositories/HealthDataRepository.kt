package com.diracsens.fallprevention.repositories

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import com.diracsens.fallprevention.database.AppDatabase
import com.diracsens.fallprevention.models.BalanceReading
import com.diracsens.fallprevention.models.BloodPressureReading
import com.diracsens.fallprevention.models.BreathingRateReading
import com.diracsens.fallprevention.models.GaitReading
import com.diracsens.fallprevention.models.HeartRateReading
import com.diracsens.fallprevention.services.BluetoothService
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

    // Export data to CSV
    suspend fun exportDataToCSV(context: Context, dataType: String): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "${dataType}_data_$timestamp.csv"

                // Create file in Downloads directory
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)

                FileWriter(file).use { writer ->
                    val csvWriter = CSVWriter(writer)

                    when (dataType) {
                        BluetoothService.DATA_TYPE_BLOOD_PRESSURE -> {
                            // Write header
                            csvWriter.writeNext(arrayOf("Timestamp", "Systolic", "Diastolic"))

                            // Write data
                            val readings = database.bloodPressureDao().getAllReadingsSync()
                            for (reading in readings) {
                                val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                    .format(Date(reading.timestamp))
                                csvWriter.writeNext(arrayOf(
                                    date,
                                    reading.systolic.toString(),
                                    reading.diastolic.toString()
                                ))
                            }
                        }
                        BluetoothService.DATA_TYPE_HEART_RATE -> {
                            // Write header
                            csvWriter.writeNext(arrayOf("Timestamp", "Heart Rate"))

                            // Write data
                            val readings = database.heartRateDao().getAllReadingsSync()
                            for (reading in readings) {
                                val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                    .format(Date(reading.timestamp))
                                csvWriter.writeNext(arrayOf(
                                    date,
                                    reading.heartRate.toString()
                                ))
                            }
                        }
                        BluetoothService.DATA_TYPE_BREATHING_RATE -> {
                            // Write header
                            csvWriter.writeNext(arrayOf("Timestamp", "Breathing Rate"))

                            // Write data
                            val readings = database.breathingRateDao().getAllReadingsSync()
                            for (reading in readings) {
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
                            val readings = database.balanceDao().getAllReadingsSync()
                            for (reading in readings) {
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
                            val readings = database.gaitDao().getAllReadingsSync()
                            for (reading in readings) {
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
                Log.e("HealthDataRepository", "Error exporting data: ${e.message}")
                null
            }
        }
    }
}