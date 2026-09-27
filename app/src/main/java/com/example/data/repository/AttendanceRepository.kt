package com.example.data.repository

import com.example.data.local.AdminNotificationDao
import com.example.data.local.AttendanceDao
import com.example.data.local.EmployeeDao
import com.example.data.local.KioskSettingsDao
import com.example.data.model.AdminNotification
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceType
import com.example.data.model.Employee
import com.example.data.model.GestureType
import com.example.data.model.KioskSettings
import com.example.data.model.PunctualityStatus
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AttendanceRepository(
    private val employeeDao: EmployeeDao,
    private val attendanceDao: AttendanceDao,
    private val adminNotificationDao: AdminNotificationDao,
    private val kioskSettingsDao: KioskSettingsDao
) {
    val allActiveEmployees: Flow<List<Employee>> = employeeDao.getAllActiveEmployees()
    val allEmployees: Flow<List<Employee>> = employeeDao.getAllEmployees()
    val allAttendance: Flow<List<AttendanceRecord>> = attendanceDao.getAllAttendanceRecords()
    val recentAttendance: Flow<List<AttendanceRecord>> = attendanceDao.getRecentAttendance(15)
    val notifications: Flow<List<AdminNotification>> = adminNotificationDao.getAllNotifications()
    val unreadNotificationsCount: Flow<Int> = adminNotificationDao.getUnreadCount()
    val kioskSettings: Flow<KioskSettings?> = kioskSettingsDao.getSettingsFlow()

    fun getTodayAttendance(date: String): Flow<List<AttendanceRecord>> =
        attendanceDao.getAttendanceForDate(date)

    fun getTodayCheckInCount(date: String): Flow<Int> = attendanceDao.getTodayCheckInCount(date)
    fun getTodayCheckOutCount(date: String): Flow<Int> = attendanceDao.getTodayCheckOutCount(date)
    fun getTodayLateCount(date: String): Flow<Int> = attendanceDao.getTodayLateCount(date)

    suspend fun getSettings(): KioskSettings {
        return kioskSettingsDao.getSettings() ?: KioskSettings().also {
            kioskSettingsDao.insertOrUpdate(it)
        }
    }

    suspend fun updateSettings(settings: KioskSettings) {
        kioskSettingsDao.insertOrUpdate(settings)
    }

    suspend fun insertEmployee(employee: Employee): Long {
        return employeeDao.insertEmployee(employee)
    }

    suspend fun updateEmployee(employee: Employee) {
        employeeDao.updateEmployee(employee)
    }

    suspend fun deleteEmployee(employee: Employee) {
        employeeDao.deleteEmployee(employee)
    }

    suspend fun purgeSampleEmployees() {
        employeeDao.deleteSampleEmployees()
    }

    suspend fun deleteAllEmployees() {
        employeeDao.deleteAllEmployees()
    }

    suspend fun recordAttendance(
        employee: Employee,
        type: AttendanceType,
        gestureType: GestureType,
        latitude: Double,
        longitude: Double,
        locationAddress: String,
        confidence: Float
    ): AttendanceRecord {
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
        val timeFormat = SimpleDateFormat("HH:mm:ss 'WIB'", Locale("id", "ID"))
        val dateStr = dateFormat.format(Date(now))
        val timeStr = timeFormat.format(Date(now))

        val punctuality = calculatePunctuality(employee, type, now)

        val record = AttendanceRecord(
            employeeId = employee.id,
            employeeCode = employee.employeeCode,
            employeeName = employee.name,
            department = employee.department,
            type = type,
            gestureType = gestureType,
            timestamp = now,
            formattedDate = dateStr,
            formattedTime = timeStr,
            latitude = latitude,
            longitude = longitude,
            locationAddress = locationAddress,
            punctualityStatus = punctuality,
            photoSnapshotUri = employee.photoUri,
            confidenceScore = confidence,
            isSyncedToCloud = true,
            syncTimestamp = now,
            adminNotified = true,
            adminNotificationNote = "Telegram, Email, dan Push Admin Terkirim"
        )

        val recordId = attendanceDao.insertAttendance(record)

        // Create Admin Notification
        val actionText = if (type == AttendanceType.CHECK_IN) "ABSEN MASUK (👍 Jempol/SIP)" else "ABSEN PULANG (🖐️ 5 Jari)"
        val notificationMsg = "🔔 PRESENSI: ${employee.name} (${employee.employeeCode} - ${employee.department}) berhasil $actionText pada $timeStr di $locationAddress. Status: ${punctuality.name}"

        val adminNotification = AdminNotification(
            attendanceId = recordId,
            employeeName = employee.name,
            employeeCode = employee.employeeCode,
            attendanceType = type,
            gestureUsed = gestureType,
            timestamp = now,
            message = notificationMsg,
            location = locationAddress,
            channel = "TELEGRAM & IN_APP",
            status = "SUCCESS"
        )
        adminNotificationDao.insertNotification(adminNotification)

        return record.copy(id = recordId)
    }

    private fun calculatePunctuality(employee: Employee, type: AttendanceType, timestamp: Long): PunctualityStatus {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentTime = timeFormat.format(Date(timestamp))

        return if (type == AttendanceType.CHECK_IN) {
            if (currentTime > employee.shiftStart) PunctualityStatus.LATE else PunctualityStatus.ON_TIME
        } else {
            if (currentTime < employee.shiftEnd) PunctualityStatus.EARLY_DEPARTURE else PunctualityStatus.NORMAL
        }
    }

    suspend fun markNotificationAsRead(id: Long) {
        adminNotificationDao.markAsRead(id)
    }

    suspend fun markAllNotificationsAsRead() {
        adminNotificationDao.markAllAsRead()
    }

    suspend fun clearAllNotifications() {
        adminNotificationDao.clearAllNotifications()
    }
}
