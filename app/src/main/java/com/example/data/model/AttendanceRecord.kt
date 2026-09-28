package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val employeeId: Long,
    val employeeCode: String,
    val employeeName: String,
    val department: String,
    val type: AttendanceType,
    val gestureType: GestureType,
    val timestamp: Long = System.currentTimeMillis(),
    val formattedDate: String, // e.g. "27 Sep 2026"
    val formattedTime: String, // e.g. "08:15:22 WIB"
    val latitude: Double,
    val longitude: Double,
    val locationAddress: String,
    val punctualityStatus: PunctualityStatus,
    val photoSnapshotUri: String = "",
    val confidenceScore: Float = 0.95f,
    val isSyncedToCloud: Boolean = true,
    val syncTimestamp: Long = System.currentTimeMillis(),
    val adminNotified: Boolean = true,
    val adminNotificationNote: String = "Telegram, WhatsApp & Cloud terkirim"
)

@Entity(tableName = "admin_notifications")
data class AdminNotification(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val attendanceId: Long,
    val employeeName: String,
    val employeeCode: String,
    val attendanceType: AttendanceType,
    val gestureUsed: GestureType,
    val timestamp: Long = System.currentTimeMillis(),
    val message: String,
    val location: String,
    val channel: String, // "TELEGRAM", "WHATSAPP", "EMAIL", "IN_APP"
    val status: String,  // "SUCCESS", "PENDING", "FAILED"
    val isRead: Boolean = false
)

@Entity(tableName = "kiosk_settings")
data class KioskSettings(
    @PrimaryKey
    val id: Int = 1,
    val kioskName: String = "Wall Tablet Kiosk - Lobby Utama",
    val companyName: String = "PT Digital Nusantara Utama",
    val officeLatitude: Double = -6.2088,
    val officeLongitude: Double = 106.8456,
    val officeRadiusMeters: Double = 150.0,
    val officeAddress: String = "Gedung Cyber Tower Lt. 12, Kuningan, Jakarta Selatan",
    val geofenceStrict: Boolean = false,
    val googleSheetUrl: String = "",
    val adminTelegramChatId: String = "",
    val adminTelegramBotToken: String = "",
    val adminTelegramWebhookUrl: String = "https://api.telegram.org/bot/notify",
    val soundVoiceFeedback: Boolean = true,
    val faceConfidenceThreshold: Float = 0.85f,
    val autoSyncToCloud: Boolean = true,
    val kioskPin: String = "1234",
    val frontCameraSelected: Boolean = true,
    val adminWhatsappNumber: String = "",
    val adminWhatsappToken: String = "",
    val adminWhatsappGatewayUrl: String = "https://api.fonnte.com/send",
    val shift1Start: String = "08:00",
    val shift1End: String = "17:00",
    val shift2Start: String = "14:00",
    val shift2End: String = "22:00",
    val shift3Start: String = "22:00",
    val shift3End: String = "06:00"
)
