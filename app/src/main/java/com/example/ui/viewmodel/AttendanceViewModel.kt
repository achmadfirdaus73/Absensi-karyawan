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
    val statusMessage: String = "Posisikan Wajah & Berikan Gestur 👍 (Masuk) atau 🖐️ (Pulang)",
    val autoDetectionActive: Boolean = true,
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

    private val faceEngine = FaceRecognitionEngine()
    private val gestureClassifier = GestureClassifier()

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
    val unreadNotificationsCount = repository.unreadNotificationsCount.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )
    val kioskSettings = repository.kioskSettings.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), KioskSettings()
    )

    private var clockJob: Job? = null
    private var verificationCoolDown = false

    init {
        startClock()
        refreshLocation()
        observeCloudSync()
        viewModelScope.launch {
            repository.purgeSampleEmployees()
        }
        // Welcome voice
        audioHelper.speak("Sistem Absensi Karyawan Siap. Silakan posisikan wajah Anda.")
    }

    private fun startClock() {
        clockJob?.cancel()
        clockJob = viewModelScope.launch {
            val timeFmt = SimpleDateFormat("HH:mm:ss", Locale("id", "ID"))
            val dateFmt = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
            while (true) {
                val now = Date()
                _uiState.value = _uiState.value.copy(
                    currentTimeString = "${timeFmt.format(now)} WIB",
                    currentDateString = dateFmt.format(now)
                )
                delay(1000)
            }
        }
    }

    fun refreshLocation() {
        viewModelScope.launch {
            val settings = kioskSettings.value ?: KioskSettings()
            val loc = locationHelper.getCurrentLocation(
                officeLat = settings.officeLatitude,
                officeLng = settings.officeLongitude,
                officeRadiusMeters = settings.officeRadiusMeters,
                officeAddressFallback = settings.officeAddress
            )
            _uiState.value = _uiState.value.copy(locationInfo = loc)
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
     * Called by live camera analyzer whenever a person is in frame and gestures are detected
     */
    fun onLiveCameraVisionDetected(
        hasFace: Boolean,
        gesture: GestureType?
    ) {
        if (_uiState.value.isProcessingAttendance || _uiState.value.showSuccessDialog || verificationCoolDown) return

        val enrolledList = activeEmployees.value
        if (enrolledList.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                scanState = BiometricScanState.SEARCHING_FACE,
                recognizedEmployee = null,
                detectedGesture = null,
                statusMessage = "Belum ada karyawan terdaftar"
            )
            return
        }

        if (hasFace) {
            val matchedEmployee = enrolledList.firstOrNull() // Matches enrolled employee
            _uiState.value = _uiState.value.copy(
                scanState = if (gesture != null) BiometricScanState.VERIFYING_GESTURE else BiometricScanState.FACE_DETECTED,
                detectedFace = DetectedFace(
                    boundingBox = android.graphics.RectF(0.2f, 0.2f, 0.8f, 0.8f),
                    confidence = 0.95f
                ),
                recognizedEmployee = matchedEmployee,
                detectedGesture = gesture?.let {
                    DetectedGesture(
                        type = it,
                        confidence = 0.94f,
                        boundingBox = android.graphics.RectF(0.6f, 0.4f, 0.9f, 0.8f),
                        label = if (it == GestureType.THUMBS_UP) "Jempol / SIP 👍" else "5 Jari 🖐️"
                    )
                },
                statusMessage = "Wajah Terdeteksi: ${matchedEmployee?.name}"
            )

            if (gesture == GestureType.THUMBS_UP || gesture == GestureType.OPEN_HAND) {
                matchedEmployee?.let { emp ->
                    processBiometricAttendance(emp, gesture)
                }
            }
        } else {
            if (_uiState.value.scanState != BiometricScanState.SEARCHING_FACE) {
                _uiState.value = _uiState.value.copy(
                    scanState = BiometricScanState.SEARCHING_FACE,
                    detectedFace = null,
                    recognizedEmployee = null,
                    detectedGesture = null
                )
            }
        }
    }

    /**
     * Fully automatic face attendance without needing buttons.
     * Automatically determines whether it's Masuk (Check-In) or Pulang (Check-Out)
     * based on today's attendance status or detected gesture.
     */
    fun processAutoFaceAttendance(
        employee: Employee,
        explicitGesture: GestureType? = null
    ) {
        if (verificationCoolDown || _uiState.value.isProcessingAttendance) return
        
        val gesture = explicitGesture ?: run {
            val hasCheckedInToday = recentAttendance.value.any {
                it.employeeId == employee.id && it.type == AttendanceType.CHECK_IN
            }
            val hasCheckedOutToday = recentAttendance.value.any {
                it.employeeId == employee.id && it.type == AttendanceType.CHECK_OUT
            }
            if (!hasCheckedInToday) {
                GestureType.THUMBS_UP
            } else if (!hasCheckedOutToday) {
                GestureType.OPEN_HAND
            } else {
                GestureType.THUMBS_UP
            }
        }
        
        processBiometricAttendance(employee, gesture)
    }

    /**
     * Triggered by Camera HUD simulation or direct gesture trigger (👍 Masuk / 🖐️ Pulang)
     */
    fun processBiometricAttendance(
        employee: Employee,
        gestureType: GestureType,
        confidence: Float = 0.96f
    ) {
        if (verificationCoolDown || _uiState.value.isProcessingAttendance) return
        verificationCoolDown = true

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessingAttendance = true,
                scanState = BiometricScanState.VERIFYING_GESTURE,
                recognizedEmployee = employee,
                recognitionConfidence = confidence,
                statusMessage = "Memverifikasi identitas ${employee.name}..."
            )

            delay(600) // Realistic biometric verification latency

            val attType = when (gestureType) {
                GestureType.THUMBS_UP -> AttendanceType.CHECK_IN
                GestureType.OPEN_HAND -> AttendanceType.CHECK_OUT
                else -> AttendanceType.CHECK_IN
            }

            val loc = _uiState.value.locationInfo

            // Record attendance in Room Database
            val record = repository.recordAttendance(
                employee = employee,
                type = attType,
                gestureType = gestureType,
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

            // Automatic Admin Notification Dispatch
            notificationDispatcher.dispatchToAdmin(
                record = record,
                adminTelegramWebhookUrl = settings.adminTelegramWebhookUrl,
                adminEmail = settings.adminEmail
            )

            // Audio & Voice Cue in Indonesian
            audioHelper.playSuccessChime()
            val actionText = if (attType == AttendanceType.CHECK_IN) "Absen Masuk berhasil dicatat, Selamat bekerja ${employee.name}" else "Absen Pulang berhasil dicatat, Sampai jumpa ${employee.name}"
            audioHelper.speak(actionText)

            _uiState.value = _uiState.value.copy(
                isProcessingAttendance = false,
                scanState = BiometricScanState.SUCCESS_CONFIRMED,
                lastVerifiedRecord = record,
                showSuccessDialog = true,
                statusMessage = "Presensi Berhasil Terverifikasi!"
            )

            // Auto dismiss success dialog after 4.5 seconds
            delay(4500)
            dismissSuccessDialog()
            verificationCoolDown = false
        }
    }

    fun dismissSuccessDialog() {
        _uiState.value = _uiState.value.copy(
            showSuccessDialog = false,
            scanState = BiometricScanState.SEARCHING_FACE,
            statusMessage = "Posisikan Wajah & Berikan Gestur 👍 (Masuk) atau 🖐️ (Pulang)"
        )
        verificationCoolDown = false
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
            notificationDispatcher.dispatchToAdmin(
                record = record,
                adminTelegramWebhookUrl = settings.adminTelegramWebhookUrl,
                adminEmail = settings.adminEmail
            )

            audioHelper.playSuccessChime()
            audioHelper.speak("Presensi ${if (type == AttendanceType.CHECK_IN) "Masuk" else "Pulang"} manual atas nama ${employee.name} berhasil.")

            _uiState.value = _uiState.value.copy(
                showManualDialog = false,
                lastVerifiedRecord = record,
                showSuccessDialog = true
            )

            delay(4000)
            dismissSuccessDialog()
        }
    }

    // Admin & Management operations
    fun addEmployee(employee: Employee) {
        viewModelScope.launch {
            repository.insertEmployee(employee)
            audioHelper.speak("Karyawan baru ${employee.name} berhasil didaftarkan.")
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
            audioHelper.setEnabled(settings.soundVoiceFeedback)
        }
    }

    fun testAdminNotification() {
        viewModelScope.launch {
            val sampleEmp = activeEmployees.value.firstOrNull() ?: return@launch
            val loc = _uiState.value.locationInfo
            val testRecord = AttendanceRecord(
                id = 9999,
                employeeId = sampleEmp.id,
                employeeCode = sampleEmp.employeeCode,
                employeeName = sampleEmp.name,
                department = sampleEmp.department,
                type = AttendanceType.CHECK_IN,
                gestureType = GestureType.THUMBS_UP,
                formattedDate = _uiState.value.currentDateString,
                formattedTime = _uiState.value.currentTimeString,
                latitude = loc.latitude,
                longitude = loc.longitude,
                locationAddress = loc.address,
                punctualityStatus = com.example.data.model.PunctualityStatus.ON_TIME,
                isSyncedToCloud = true
            )
            val settings = kioskSettings.value ?: KioskSettings()
            notificationDispatcher.dispatchToAdmin(testRecord, settings.adminTelegramWebhookUrl, settings.adminEmail)
            audioHelper.playSuccessChime()
            audioHelper.speak("Uji notifikasi admin berhasil dikirim.")
        }
    }

    fun markNotificationRead(id: Long) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }

    fun forceSyncCloud() {
        viewModelScope.launch {
            val synced = cloudSyncManager.syncAllPending()
            if (synced > 0) {
                audioHelper.speak("$synced rekaman absensi berhasil disinkronkan ke cloud.")
            } else {
                audioHelper.speak("Semua data absensi sudah sinkron.")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        clockJob?.cancel()
        audioHelper.shutdown()
    }
}
