package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.repository.AttendanceRepository
import com.example.network.AdminNotificationDispatcher
import com.example.network.CloudSyncManager

class AttendanceKioskApp : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy {
        AttendanceRepository(
            database.employeeDao(),
            database.attendanceDao(),
            database.adminNotificationDao(),
            database.kioskSettingsDao()
        )
    }
    val cloudSyncManager by lazy {
        CloudSyncManager(this, database.attendanceDao())
    }
    val notificationDispatcher by lazy {
        AdminNotificationDispatcher(this)
    }

    override fun onCreate() {
        super.onCreate()
    }
}
