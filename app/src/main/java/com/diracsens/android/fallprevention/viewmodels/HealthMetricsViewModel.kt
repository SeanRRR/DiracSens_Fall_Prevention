package com.diracsens.android.fallprevention.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
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
import com.diracsens.android.fallprevention.repositories.HealthDataRepository
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

    // Chromium
    private val _currentChromium = MutableLiveData<Float>()
    val currentChromium: LiveData<Float> = _currentChromium

    val chromiumHistory: LiveData<List<ChromiumReading>> = repository.getAllChromiumReadings()
    val recentChromium: LiveData<List<ChromiumReading>> = repository.getRecentChromiumReadings(10)

    // Lead
    private val _currentLead = MutableLiveData<Float>()
    val currentLead: LiveData<Float> = _currentLead

    val leadHistory: LiveData<List<LeadReading>> = repository.getAllLeadReadings()
    val recentLead: LiveData<List<LeadReading>> = repository.getRecentLeadReadings(10)

    // Mercury
    private val _currentMercury = MutableLiveData<Float>()
    val currentMercury: LiveData<Float> = _currentMercury

    val mercuryHistory: LiveData<List<MercuryReading>> = repository.getAllMercuryReadings()
    val recentMercury: LiveData<List<MercuryReading>> = repository.getRecentMercuryReadings(10)

    // Cadmium
    private val _currentCadmium = MutableLiveData<Float>()
    val currentCadmium: LiveData<Float> = _currentCadmium

    val cadmiumHistory: LiveData<List<CadmiumReading>> = repository.getAllCadmiumReadings()
    val recentCadmium: LiveData<List<CadmiumReading>> = repository.getRecentCadmiumReadings(10)

    // Silver
    private val _currentSilver = MutableLiveData<Float>()
    val currentSilver: LiveData<Float> = _currentSilver

    val silverHistory: LiveData<List<SilverReading>> = repository.getAllSilverReadings()
    val recentSilver: LiveData<List<SilverReading>> = repository.getRecentSilverReadings(10)

    // Temperature
    private val _currentTemperature = MutableLiveData<Float>()
    val currentTemperature: LiveData<Float> = _currentTemperature

    val temperatureHistory: LiveData<List<TemperatureReading>> = repository.getAllTemperatureReadings()
    val recentTemperature: LiveData<List<TemperatureReading>> = repository.getRecentTemperatureReadings(10)

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

    fun deleteAllHeartRateReadings() {
        viewModelScope.launch {
            repository.deleteAllHeartRateReadings()
            _currentHeartRate.value = null
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

    // Update methods for new sensors
    fun updateChromium(value: Float) {
        _currentChromium.value = value
        viewModelScope.launch {
            repository.insertChromiumReading(
                ChromiumReading(
                    timestamp = System.currentTimeMillis(),
                    value = value
                )
            )
        }
    }

    fun updateLead(value: Float) {
        _currentLead.value = value
        viewModelScope.launch {
            repository.insertLeadReading(
                LeadReading(
                    timestamp = System.currentTimeMillis(),
                    value = value
                )
            )
        }
    }

    fun updateMercury(value: Float) {
        _currentMercury.value = value
        viewModelScope.launch {
            repository.insertMercuryReading(
                MercuryReading(
                    timestamp = System.currentTimeMillis(),
                    value = value
                )
            )
        }
    }

    fun updateCadmium(value: Float) {
        _currentCadmium.value = value
        viewModelScope.launch {
            repository.insertCadmiumReading(
                CadmiumReading(
                    timestamp = System.currentTimeMillis(),
                    value = value
                )
            )
        }
    }

    fun updateSilver(value: Float) {
        _currentSilver.value = value
        viewModelScope.launch {
            repository.insertSilverReading(
                SilverReading(
                    timestamp = System.currentTimeMillis(),
                    value = value
                )
            )
        }
    }

    fun updateTemperature(value: Float) {
        _currentTemperature.value = value
        viewModelScope.launch {
            repository.insertTemperatureReading(
                TemperatureReading(
                    timestamp = System.currentTimeMillis(),
                    value = value
                )
            )
        }
    }

    fun updateHeartRateBatch(heartRates: IntArray) {
        heartRates.forEach { heartRate ->
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
    }

    // Export data
    fun exportData(dataType: String, onComplete: (Uri?) -> Unit) {
        viewModelScope.launch {
            val uri = repository.exportDataToCSV(getApplication(), dataType)
            onComplete(uri)
        }
    }
}