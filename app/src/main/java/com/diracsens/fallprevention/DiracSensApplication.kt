// DiracSensApplication.kt
package com.diracsens.fallprevention

import android.app.Application
import com.diracsens.fallprevention.repositories.HealthDataRepository

class DiracSensApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize repositories
        HealthDataRepository.initialize(this)
    }
}