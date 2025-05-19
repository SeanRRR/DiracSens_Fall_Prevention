package com.diracsens.fallprevention.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blood_pressure_readings")
data class BloodPressureReading(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val systolic: Int,
    val diastolic: Int
)

@Entity(tableName = "heart_rate_readings")
data class HeartRateReading(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val heartRate: Int
)

@Entity(tableName = "breathing_rate_readings")
data class BreathingRateReading(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val breathingRate: Int
)

@Entity(tableName = "gait_readings")
data class GaitReading(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val walkingSpeed: Float,
    val stepLength: Float,
    val stepLengthVariability: Float,
    val lateralSway: Float
)

@Entity(tableName = "balance_readings")
data class BalanceReading(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val swayArea: Float,
    val swayVelocity: Float,
    val anteriorPosteriorSway: Float,
    val medialLateralSway: Float
)