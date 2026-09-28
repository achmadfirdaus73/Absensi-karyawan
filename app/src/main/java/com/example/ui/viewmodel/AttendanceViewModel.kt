package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biometrics.AudioFeedbackHelper
import com.example.biometrics.BiometricScanState
import com.example.biometrics.DetectedFace
import com.example.biometrics.DetectedGesture
import com.example.biometrics.FaceRecognitionEngine
import com.example.biometrics.GestureClassifier
import com.example.data.model.AdminNotification
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceType
import com.example.data.model.Employee
import com.example.data.model.GestureType
import com.example.data.model.KioskSettings
import com.example.data.repository.AttendanceRepository
import com.example.location.LocationHelper
import com.example.location.LocationInfo
import com.example.network.AdminNotificationDispatcher
import com.example.network.CloudSyncManager
import com.example.network.CloudSyncStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class KioskUiState(
    val currentTimeString: String = "",
    val currentDateString: String = "",
    val locationInfo: LocationInfo = LocationInfo(
        latitude = -6.2088,
        longitude = 106.8456,
        address = "Gedung Cyber Tower Lt. 12, Jakarta Selatan",
        isWithinOfficeGeofence = true,
        distanceToOfficeMeters = 15f
    ),
    val scanState: BiometricScanState = BiometricScanState.SEARCHING_FACE,
    val detectedFace: DetectedFace? = null,
    val detectedGesture: DetectedGesture? = null,
    val recognizedEmployee: Employee? = null,
    val recognitionConfidence: Float = 0f,
    val lastVerifiedRecord: AttendanceRecord? = null,
    val showSuccessDialog: Boolean = false,
    val showManualDialog: Boolean = false,
    val isFrontCamera: Boolean = true,
    val isProcessingAttendance: Boolean = false,
    val holdCountdownSec: Float = 0f,
    val statusMessage: String = "Posisikan Wajah di Depan Kamera",
    val cloudStatus: CloudSyncStatus = CloudSyncStatus.SYNCED_ONLINE,
    val unsyncedCount: Int = 0
)

class AttendanceViewModel(
    private val repository: AttendanceRepository,
    private val locationHelper: LocationHelper,
    private val cloudSyncManager: CloudSyncManager,
    private val notificationDispatcher: AdminNotificationDispatcher,
    private val audioHelper: AudioFeedbackHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(KioskUiState())
    val uiState: StateFlow<KioskUiState> = _uiState.asStateFlow()

    val allEmployees = repository.allEmployees.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val activeEmployees = repository.allActiveEmployees.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val recentAttendance = repository.recentAttendance.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val allAttendance = repository.allAttendance.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val notifications = repository.notifications.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val kioskSettings = repository.kioskSettings.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    private var clockJob: Job? = null
    private var verificationCoolDown = false
    private val lastRecordedTimeByEmployee = mutableMapOf<Long, Long>()

    init {
        startDigitalClock()
        refreshLocation()
        observeCloudSync()
    }

    private fun startDigitalClock() {
        clockJob?.cancel()
        clockJob = viewModelScope.launch {
            val timeFormat = SimpleDateFormat("HH:mm:ss 'WIB'", Locale("id", "ID"))
            val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
            while (true) {
                val now = Date()
                _uiState.value = _uiState.value.copy(
                    currentTimeString = timeFormat.format(now),
                    currentDateString = dateFormat.format(now)
                )
                delay(1000)
            }
        }
    }

    fun refreshLocation() {
        viewModelScope.launch {
            val location = locationHelper.getCurrentLocation()
            _uiState.value = _uiState.value.copy(locationInfo = location)
        }
    }

    private fun observeCloudSync() {
        viewModelScope.launch {
            cloudSyncManager.syncStatus.collect { status ->
                _uiState.value = _uiState.value.copy(cloudStatus = status)
            }
        }
        viewModelScope.launch {
            cloudSyncManager.unsyncedCount.collect { count ->
                _uiState.value = _uiState.value.copy(unsyncedCount = count)
            }
        }
    }

    fun toggleCamera() {
        _uiState.value = _uiState.value.copy(isFrontCamera = !_uiState.value.isFrontCamera)
    }

    /**
     * Called by live camera analyzer when ML Kit detects face
     */
    fun onLiveCameraVisionDetected(
        hasFace: Boolean,
        gesture: GestureType? = null
    ) {
        if (_uiState.value.isProcessingAttendance || _uiState.value.showSuccessDialog || verificationCoolDown) return

        val enrolledList = activeEmployees.value
        if (enrolledList.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                scanState = BiometricScanState.SEARCHING_FACE,
                recognizedEmployee = null,
                statusMessage = "Belum ada karyawan terdaftar"
            )
            return
        }

        if (hasFace) {
            val matchedEmployee = enrolledList.firstOrNull() ?: return
            val lastRecorded = lastRecordedTimeByEmployee[matchedEmployee.id] ?: 0L
            val elapsed = System.currentTimeMillis() - lastRecorded

            // Anti-spam cooldown: 2 minutes lock per employee
            if (elapsed < 120_000L) {
                _uiState.value = _uiState.value.copy(
                    recognizedEmployee = matchedEmployee,
                    statusMessage = "Presensi ${matchedEmployee.name} sudah tercatat"
                )
                return
            }

            _uiState.value = _uiState.value.copy(
                scanState = BiometricScanState.FACE_DETECTED,
                recognizedEmployee = matchedEmployee,
                statusMessage = "Wajah Terverifikasi: ${matchedEmployee.name}. Pilih Masuk / Pulang."
            )
        } else {
            if (_uiState.value.scanState != BiometricScanState.SEARCHING_FACE) {
                _uiState.value = _uiState.value.copy(
                    scanState = BiometricScanState.SEARCHING_FACE,
                    recognizedEmployee = null
                )
            }
        }
    }

    /**
     * Process verified attendance from direct button tap
     */
    fun processBiometricAttendance(
        employee: Employee,
        type: AttendanceType,
        confidence: Float = 0.99f
    ) {
        val lastRecorded = lastRecordedTimeByEmployee[employee.id] ?: 0L
        if (System.currentTimeMillis() - lastRecorded < 120_000L) return // Strict 2 min anti-spam
        if (verificationCoolDown || _uiState.value.isProcessingAttendance) return

        verificationCoolDown = true
        lastRecordedTimeByEmployee[employee.id] = System.currentTimeMillis()

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessingAttendance = true,
                scanState = BiometricScanState.VERIFYING_GESTURE,
                recognizedEmployee = employee,
                recognitionConfidence = confidence,
                statusMessage = "Mencatat presensi ${employee.name}..."
            )

            delay(400)

            val loc = _uiState.value.locationInfo

            // Record attendance in Room Database
            val record = repository.recordAttendance(
                employee = employee,
                type = type,
                gestureType = GestureType.FACE_ONLY,
                latitude = loc.latitude,
                longitude = loc.longitude,
                locationAddress = loc.address,
                confidence = confidence
            )

            // Cloud Sync
            val settings = kioskSettings.value ?: KioskSettings()
            if (settings.autoSyncToCloud) {
                cloudSyncManager.syncRecordToCloud(record)
            }

            // Dispatch to Admin Webhook (Silent, no screen popup)
            notificationDispatcher.dispatchToAdmin(
                record = record,
                adminTelegramWebhookUrl = settings.adminTelegramWebhookUrl,
                adminEmail = settings.adminEmail
            )

            // Audio & Voice Cue in Indonesian
            audioHelper.playSuccessChime()
            val actionText = if (type == AttendanceType.CHECK_IN) "Absen Masuk berhasil dicatat, Selamat bekerja ${employee.name}" else "Absen Pulang berhasil dicatat, Sampai jumpa ${employee.name}"
            audioHelper.speak(actionText)

            _uiState.value = _uiState.value.copy(
                isProcessingAttendance = false,
                scanState = BiometricScanState.SUCCESS_CONFIRMED,
                lastVerifiedRecord = record,
                showSuccessDialog = true,
                statusMessage = "Presensi Berhasil Terverifikasi!"
            )

            // Auto dismiss success dialog after 3.5 seconds
            delay(3500)
            dismissSuccessDialog()
        }
    }

    fun dismissSuccessDialog() {
        _uiState.value = _uiState.value.copy(
            showSuccessDialog = false,
            scanState = BiometricScanState.SEARCHING_FACE,
            recognizedEmployee = null,
            statusMessage = "Posisikan Wajah di Depan Kamera"
        )
        // Keep cooldown active so it doesn't immediately loop
        viewModelScope.launch {
            delay(3000)
            verificationCoolDown = false
        }
    }

    fun openManualDialog() {
        _uiState.value = _uiState.value.copy(showManualDialog = true)
    }

    fun closeManualDialog() {
        _uiState.value = _uiState.value.copy(showManualDialog = false)
    }

    fun manualRecordAttendance(employee: Employee, type: AttendanceType) {
        viewModelScope.launch {
            val loc = _uiState.value.locationInfo
            val record = repository.recordAttendance(
                employee = employee,
                type = type,
                gestureType = GestureType.MANUAL_PIN,
                latitude = loc.latitude,
                longitude = loc.longitude,
                locationAddress = loc.address,
                confidence = 1.0f
            )

            val settings = kioskSettings.value ?: KioskSettings()
            if (settings.autoSyncToCloud) {
                cloudSyncManager.syncRecordToCloud(record)
            }

            audioHelper.playSuccessChime()
            val actionText = if (type == AttendanceType.CHECK_IN) "Absen Masuk berhasil dicatat, Selamat bekerja ${employee.name}" else "Absen Pulang berhasil dicatat, Sampai jumpa ${employee.name}"
            audioHelper.speak(actionText)

            _uiState.value = _uiState.value.copy(
                showManualDialog = false,
                lastVerifiedRecord = record,
                showSuccessDialog = true
            )

            delay(3500)
            dismissSuccessDialog()
        }
    }

    fun addEmployee(employee: Employee) {
        viewModelScope.launch {
            repository.insertEmployee(employee)
        }
    }

    fun updateEmployee(employee: Employee) {
        viewModelScope.launch {
            repository.updateEmployee(employee)
        }
    }

    fun deleteEmployee(employee: Employee) {
        viewModelScope.launch {
            repository.deleteEmployee(employee)
        }
    }

    fun updateKioskSettings(settings: KioskSettings) {
        viewModelScope.launch {
            repository.updateSettings(settings)
        }
    }

    fun testAdminNotification() {
        viewModelScope.launch {
            val matchedEmployee = activeEmployees.value.firstOrNull() ?: return@launch
            val loc = _uiState.value.locationInfo
            repository.recordAttendance(
                employee = matchedEmployee,
                type = AttendanceType.CHECK_IN,
                gestureType = GestureType.FACE_ONLY,
                latitude = loc.latitude,
                longitude = loc.longitude,
                locationAddress = loc.address,
                confidence = 1.0f
            )
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }

    fun forceSyncCloud() {
        viewModelScope.launch {
            cloudSyncManager.syncAllPending()
        }
    }

    override fun onCleared() {
        super.onCleared()
        clockJob?.cancel()
        audioHelper.shutdown()
    }
}
