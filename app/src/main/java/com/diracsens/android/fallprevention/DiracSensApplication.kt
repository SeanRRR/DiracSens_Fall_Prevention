// DiracSensApplication.kt
package com.diracsens.android.fallprevention

import android.app.Application
import com.diracsens.android.fallprevention.repositories.HealthDataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.diracsens.android.fallprevention.database.AppDatabase

class DiracSensApplication : Application() {

    companion object {
        var hasLaunchedOnce: Boolean = false
    }

    override fun onCreate() {
        super.onCreate()
        // Wipe all state on true cold start
        getSharedPreferences("heart_rate_prefs", MODE_PRIVATE).edit().clear().apply()
        // Wipe all sensor data tables in the database
        val db = AppDatabase.getInstance(this)
        CoroutineScope(Dispatchers.IO).launch {
            db.heartRateDao().deleteAllReadings()
            // Add similar calls for other DAOs when implemented:
            // db.bloodPressureDao().deleteAllReadings()
            // db.breathingRateDao().deleteAllReadings()
            // db.gaitDao().deleteAllReadings()
            // db.balanceDao().deleteAllReadings()
            // db.chromiumDao().deleteAllReadings()
            // db.leadDao().deleteAllReadings()
            // db.mercuryDao().deleteAllReadings()
            // db.cadmiumDao().deleteAllReadings()
            // db.silverDao().deleteAllReadings()
            // db.temperatureDao().deleteAllReadings()
        }
        // Initialize repositories
        HealthDataRepository.initialize(this)
    }
}