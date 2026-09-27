package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.AttendanceKioskApp
import com.example.biometrics.AudioFeedbackHelper
import com.example.location.LocationHelper

class AttendanceViewModelFactory(
    private val app: AttendanceKioskApp
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AttendanceViewModel::class.java)) {
            val locationHelper = LocationHelper(app)
            val audioHelper = AudioFeedbackHelper(app)
            return AttendanceViewModel(
                repository = app.repository,
                locationHelper = locationHelper,
                cloudSyncManager = app.cloudSyncManager,
                notificationDispatcher = app.notificationDispatcher,
                audioHelper = audioHelper
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
