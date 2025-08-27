package com.diracsens.android.fallprevention.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chromium_readings")
data class ChromiumReading(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val value: Float
)

@Entity(tableName = "lead_readings")
data class LeadReading(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val value: Float
)

@Entity(tableName = "mercury_readings")
data class MercuryReading(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val value: Float
)

@Entity(tableName = "cadmium_readings")
data class CadmiumReading(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val value: Float
)

@Entity(tableName = "silver_readings")
data class SilverReading(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val value: Float
)

@Entity(tableName = "temperature_readings")
data class TemperatureReading(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val value: Float
) 