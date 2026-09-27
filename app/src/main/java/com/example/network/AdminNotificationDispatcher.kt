package com.example.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceType
import com.example.data.model.GestureType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class AdminNotificationDispatcher(private val context: Context) {
    private val channelId = "admin_attendance_alerts_silent"
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Admin Attendance Sync",
                NotificationManager.IMPORTANCE_LOW // Low importance to prevent disruptive heads-up popups on camera
            ).apply {
                description = "Sinkronisasi log presensi karyawan"
                setShowBadge(false)
                enableVibration(false)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    suspend fun dispatchToAdmin(
        record: AttendanceRecord,
        adminTelegramWebhookUrl: String,
        adminEmail: String
    ): Boolean = withContext(Dispatchers.IO) {
        val gestureIcon = when (record.gestureType) {
            GestureType.THUMBS_UP -> "👍 (SIP / Jempol)"
            GestureType.OPEN_HAND -> "🖐️ (5 Jari Terbuka)"
            GestureType.FACE_ONLY -> "👤 (Biometrik Wajah)"
            GestureType.MANUAL_PIN -> "🔢 (PIN Fallback)"
        }

        val typeText = if (record.type == AttendanceType.CHECK_IN) "ABSEN MASUK" else "ABSEN PULANG"

        // Dispatch to Telegram Webhook / Cloud Alert Webhook if configured
        var webhookSuccess = false
        try {
            val jsonPayload = """
                {
                    "event": "EMPLOYEE_ATTENDANCE_VERIFIED",
                    "employee_name": "${record.employeeName}",
                    "employee_code": "${record.employeeCode}",
                    "department": "${record.department}",
                    "type": "$typeText",
                    "gesture": "$gestureIcon",
                    "timestamp": "${record.formattedDate} ${record.formattedTime}",
                    "location": "${record.locationAddress}",
                    "latitude": ${record.latitude},
                    "longitude": ${record.longitude},
                    "punctuality": "${record.punctualityStatus.name}",
                    "admin_recipient": "$adminEmail",
                    "source": "SmartPresensi Wall Kiosk AI"
                }
            """.trimIndent()

            if (adminTelegramWebhookUrl.isNotBlank() && adminTelegramWebhookUrl.startsWith("http")) {
                val body = jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                val request = Request.Builder()
                    .url(adminTelegramWebhookUrl)
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                webhookSuccess = response.isSuccessful
                response.close()
            } else {
                webhookSuccess = true
            }
        } catch (e: Exception) {
            webhookSuccess = false
        }

        webhookSuccess
    }
}
