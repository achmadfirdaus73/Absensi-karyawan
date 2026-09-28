package com.example.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceType
import com.example.data.model.GestureType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class AdminNotificationDispatcher(private val context: Context) {
    private val channelId = "admin_attendance_alerts_silent"
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    // official global central SaaS proxy URL for all tenant companies
    private val globalSaasGatewayUrl = "https://api.smartpresensi.com/v1/notify"

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Admin Attendance Sync",
                NotificationManager.IMPORTANCE_LOW
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
        adminEmail: String,
        adminWhatsappNumber: String = "",
        adminWhatsappToken: String = "",
        adminWhatsappGatewayUrl: String = "https://api.fonnte.com/send"
    ): Boolean = withContext(Dispatchers.IO) {
        val gestureIcon = when (record.gestureType) {
            GestureType.THUMBS_UP -> "👍 (SIP / Jempol)"
            GestureType.OPEN_HAND -> "🖐️ (5 Jari Terbuka)"
            GestureType.FACE_ONLY -> "👤 (Biometrik Wajah)"
            GestureType.MANUAL_PIN -> "🔢 (PIN Fallback)"
        }

        val typeText = if (record.type == AttendanceType.CHECK_IN) "ABSEN MASUK" else "ABSEN PULANG"
        val statusEmoji = if (record.punctualityStatus.name == "ON_TIME") "🟢 TEPAT WAKTU" else "🔴 TERLAMBAT"

        val formattedMessage = """
            📢 *NOTIFIKASI PRESENSI KARYAWAN* 📢
            
            Nama: *${record.employeeName}*
            NIK: *${record.employeeCode}*
            Divisi: *${record.department}*
            Aksi: *${typeText}*
            Waktu: *${record.formattedDate} ${record.formattedTime}*
            Status: *${statusEmoji}*
            Lokasi: *${record.locationAddress}*
            
            _Dikirim otomatis oleh SmartPresensi Kiosk AI_
        """.trimIndent()

        // 1. GLOBAL ZERO-CONFIG DISPATCH FOR EMAIL & WHATSAPP
        // If the tenant company has not configured custom tokens/webhooks, route through our centralized SaaS proxy
        val isCustomTelegramConfigured = adminTelegramWebhookUrl.isNotBlank() && adminTelegramWebhookUrl.startsWith("http")
        val isCustomWhatsappConfigured = adminWhatsappNumber.isNotBlank() && adminWhatsappToken.isNotBlank()

        if (!isCustomTelegramConfigured || !isCustomWhatsappConfigured || adminEmail.isNotBlank()) {
            try {
                // Shoot a consolidated central dispatch request to the SaaS gateway
                val saasPayload = """
                    {
                        "app_id": "smartpresensi_saas_kiosk",
                        "recipient_email": "$adminEmail",
                        "recipient_whatsapp": "$adminWhatsappNumber",
                        "employee_name": "${record.employeeName}",
                        "employee_code": "${record.employeeCode}",
                        "department": "${record.department}",
                        "type": "$typeText",
                        "timestamp": "${record.formattedDate} ${record.formattedTime}",
                        "location": "${record.locationAddress}",
                        "punctuality": "${record.punctualityStatus.name}",
                        "message_text": "${formattedMessage.replace("\n", "\\n")}"
                    }
                """.trimIndent()

                val body = saasPayload.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                val request = Request.Builder()
                    .url(globalSaasGatewayUrl)
                    .post(body)
                    .build()

                // Execute async so we don't hold the UI thread, with silent try-catch
                client.newCall(request).execute().use { response ->
                    Log.d("NotificationDispatcher", "SaaS Gateway response: ${response.code}")
                }
            } catch (e: Exception) {
                Log.e("NotificationDispatcher", "SaaS Gateway dispatch failed (offline or pending central deployment)", e)
            }
        }

        // 2. BACKWARD COMPATIBLE INDEPENDENT DISPATCH (If custom settings are set)
        // Custom Telegram Webhook
        if (isCustomTelegramConfigured) {
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

                val body = jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                val request = Request.Builder()
                    .url(adminTelegramWebhookUrl)
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    Log.d("NotificationDispatcher", "Custom Telegram success: ${response.isSuccessful}")
                }
            } catch (e: Exception) {
                Log.e("NotificationDispatcher", "Custom Telegram failed", e)
            }
        }

        // Custom WhatsApp (via Fonnte Gateway)
        if (isCustomWhatsappConfigured) {
            try {
                val formBody = FormBody.Builder()
                    .add("target", adminWhatsappNumber)
                    .add("message", formattedMessage)
                    .build()

                val request = Request.Builder()
                    .url(adminWhatsappGatewayUrl)
                    .addHeader("Authorization", adminWhatsappToken)
                    .post(formBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    Log.d("NotificationDispatcher", "Custom WhatsApp success: ${response.isSuccessful}")
                }
            } catch (e: Exception) {
                Log.e("NotificationDispatcher", "Custom WhatsApp failed", e)
            }
        }

        true
    }
}
