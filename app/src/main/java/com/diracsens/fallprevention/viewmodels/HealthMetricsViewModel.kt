package com.diracsens.fallprevention.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.diracsens.fallprevention.models.BalanceReading
import com.diracsens.fallprevention.models.BloodPressureReading
import com.diracsens.fallprevention.models.BreathingRateReading
import com.diracsens.fallprevention.models.GaitReading
import com.diracsens.fallprevention.models.HeartRateReading
import com.diracsens.fallprevention.repositories.HealthDataRepository
import kotlinx.coroutines.launch

class HealthMetricsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: HealthDataRepository = HealthDataRepository.getInstance()

    // Blood Pressure
    private val _currentBloodPressure = MutableLiveData<Pair<Int, Int>>()
    val currentBloodPressure: LiveData<Pair<Int, Int>> = _currentBloodPressure

    val bloodPressureHistory: LiveData<List<BloodPressureReading>> = repository.getAllBloodPressureReadings()
    val recentBloodPressure: LiveData<List<BloodPressureReading>> = repository.getRecentBloodPressureReadings(10)

    // Heart Rate
    private val _currentHeartRate = MutableLiveData<Int>()
    val currentHeartRate: LiveData<Int> = _currentHeartRate

    val heartRateHistory: LiveData<List<HeartRateReading>> = repository.getAllHeartRateReadings()
    val recentHeartRate: LiveData<List<HeartRateReading>> = repository.getRecentHeartRateReadings(10)

    // Breathing Rate
    private val _currentBreathingRate = MutableLiveData<Int>()
    val currentBreathingRate: LiveData<Int> = _currentBreathingRate

    val breathingRateHistory: LiveData<List<BreathingRateReading>> = repository.getAllBreathingRateReadings()
    val recentBreathingRate: LiveData<List<BreathingRateReading>> = repository.getRecentBreathingRateReadings(10)

    // Gait() = repository.getRecentBreathingRateReadings(10)

    // Gait
    private val _currentGait = MutableLiveData<GaitReading>()
    val currentGait: LiveData<GaitReading> = _currentGait

    val gaitHistory: LiveData<List<GaitReading>> = repository.getAllGaitReadings()
    val recentGait: LiveData<List<GaitReading>> = repository.getRecentGaitReadings(10)

    // Balance
    private val _currentBalance = MutableLiveData<BalanceReading>()
    val currentBalance: LiveData<BalanceReading> = _currentBalance

    val balanceHistory: LiveData<List<BalanceReading>> = repository.getAllBalanceReadings()
    val recentBalance: LiveData<List<BalanceReading>> = repository.getRecentBalanceReadings(10)

    // Update methods
    fun updateBloodPressure(systolic: Int, diastolic: Int) {
        _currentBloodPressure.value = Pair(systolic, diastolic)
        viewModelScope.launch {
            repository.insertBloodPressure(
                BloodPressureReading(
                    timestamp = System.currentTimeMillis(),
                    systolic = systolic,
                    diastolic = diastolic
                )
            )
        }
    }

    fun updateHeartRate(heartRate: Int) {
        _currentHeartRate.value = heartRate
        viewModelScope.launch {
            repository.insertHeartRate(
                HeartRateReading(
                    timestamp = System.currentTimeMillis(),
                    heartRate = heartRate
                )
            )
        }
    }

    fun updateBreathingRate(breathingRate: Int) {
        _currentBreathingRate.value = breathingRate
        viewModelScope.launch {
            repository.insertBreathingRate(
                BreathingRateReading(
                    timestamp = System.currentTimeMillis(),
                    breathingRate = breathingRate
                )
            )
        }
    }

    fun updateGait(walkingSpeed: Float, stepLength: Float, stepLengthVariability: Float, lateralSway: Float) {
        val gaitReading = GaitReading(
            timestamp = System.currentTimeMillis(),
            walkingSpeed = walkingSpeed,
            stepLength = stepLength,
            stepLengthVariability = stepLengthVariability,
            lateralSway = lateralSway
        )
        _currentGait.value = gaitReading
        viewModelScope.launch {
            repository.insertGait(gaitReading)
        }
    }

    fun updateBalance(swayArea: Float, swayVelocity: Float, anteriorPosteriorSway: Float, medialLateralSway: Float) {
        val balanceReading = BalanceReading(
            timestamp = System.currentTimeMillis(),
            swayArea = swayArea,
            swayVelocity = swayVelocity,
            anteriorPosteriorSway = anteriorPosteriorSway,
            medialLateralSway = medialLateralSway
        )
        _currentBalance.value = balanceReading
        viewModelScope.launch {
            repository.insertBalance(balanceReading)
        }
    }

    // Export data
    fun exportData(dataType: String, onComplete: (Uri?) -> Unit) {
        viewModelScope.launch {
            val uri = repository.exportDataToCSV(getApplication(), dataType)
            onComplete(uri)
        }
    }
}