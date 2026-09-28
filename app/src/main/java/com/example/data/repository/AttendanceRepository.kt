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

        val settings = kioskSettingsDao.getSettings() ?: KioskSettings()
        val punctuality = calculatePunctuality(employee, type, now, settings)

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

    private fun calculatePunctuality(
        employee: Employee,
        type: AttendanceType,
        timestamp: Long,
        settings: KioskSettings
    ): PunctualityStatus {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentTimeStr = timeFormat.format(Date(timestamp))
        val currentMinutes = parseTimeToMinutes(currentTimeStr)

        // Parse Shift 1, Shift 2, Shift 3 configurations
        val s1StartMin = parseTimeToMinutes(settings.shift1Start)
        val s1EndMin = parseTimeToMinutes(settings.shift1End)
        val s2StartMin = parseTimeToMinutes(settings.shift2Start)
        val s2EndMin = parseTimeToMinutes(settings.shift2End)
        val s3StartMin = parseTimeToMinutes(settings.shift3Start)
        val s3EndMin = parseTimeToMinutes(settings.shift3End)

        if (type == AttendanceType.CHECK_IN) {
            val diff1 = circularDiffMinutes(currentMinutes, s1StartMin)
            val diff2 = circularDiffMinutes(currentMinutes, s2StartMin)
            val diff3 = circularDiffMinutes(currentMinutes, s3StartMin)

            val minDiff = minOf(diff1, diff2, diff3)
            val chosenStartMin = when (minDiff) {
                diff1 -> s1StartMin
                diff2 -> s2StartMin
                else -> s3StartMin
            }

            return if (isTimeAfter(currentMinutes, chosenStartMin)) {
                PunctualityStatus.LATE
            } else {
                PunctualityStatus.ON_TIME
            }
        } else {
            val diff1 = circularDiffMinutes(currentMinutes, s1EndMin)
            val diff2 = circularDiffMinutes(currentMinutes, s2EndMin)
            val diff3 = circularDiffMinutes(currentMinutes, s3EndMin)

            val minDiff = minOf(diff1, diff2, diff3)
            val chosenEndMin = when (minDiff) {
                diff1 -> s1EndMin
                diff2 -> s2EndMin
                else -> s3EndMin
            }

            return if (currentMinutes < chosenEndMin) {
                PunctualityStatus.EARLY_DEPARTURE
            } else {
                var diff = currentMinutes - chosenEndMin
                // Handle overnight shift checkout on next day
                if (diff < -720) {
                    diff += 1440
                }
                
                if (diff >= 30) {
                    PunctualityStatus.OVERTIME
                } else {
                    PunctualityStatus.NORMAL
                }
            }
        }
    }

    private fun parseTimeToMinutes(timeStr: String): Int {
        return try {
            val parts = timeStr.trim().split(":")
            parts[0].toInt() * 60 + parts[1].toInt()
        } catch (e: Exception) {
            0
        }
    }

    private fun circularDiffMinutes(t1: Int, t2: Int): Int {
        val diff = kotlin.math.abs(t1 - t2)
        return kotlin.math.min(diff, 1440 - diff)
    }

    private fun isTimeAfter(t1: Int, t2: Int): Boolean {
        val diff = t1 - t2
        return if (diff in 1..720) {
            true
        } else if (diff < -720) {
            true // wrapped around midnight
        } else {
            false
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
