// DiracSensApplication.kt
package com.diracsens.android.fallprevention

import android.app.Application
import com.diracsens.android.fallprevention.repositories.HealthDataRepository

class DiracSensApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize repositories
        HealthDataRepository.initialize(this)
    }
}