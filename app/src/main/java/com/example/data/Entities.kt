package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workers")
data class Worker(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val username: String = "",
    val password: String = "",
    val isAdmin: Boolean = false,
    val hourlyRate: Double = 15.0, // Default hourly rate in RM (remains in schema for compatibility)
    val email: String = "",
    val isActive: Boolean = true
)

@Entity(tableName = "punch_logs")
data class PunchLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val workerId: Int,
    val workerName: String,
    val date: String, // YYYY-MM-DD
    val punchInTime: Long? = null, // Timestamp in Epoch MS
    val punchOutTime: Long? = null, // Timestamp in Epoch MS
    val isLate: Boolean = false, // If punchInTime > 8:00 AM
    val isEarlyOut: Boolean = false, // If punchOutTime < 5:00 PM
    val durationMinutes: Long = 0L,	// Calculated duration in minutes
    val wifiSsid: String? = null
)

@Entity(tableName = "punch_events")
data class PunchEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val workerId: Int,
    val workerName: String,
    val date: String, // YYYY-MM-DD
    val timestamp: Long, // Epoch ms when the event happened
    val type: String, // "IN" or "OUT"
    val isLate: Boolean = false,
    val isEarlyOut: Boolean = false,
    val wifiSsid: String? = null
)

@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey val key: String,
    val value: String
)
