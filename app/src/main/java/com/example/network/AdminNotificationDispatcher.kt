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
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Real hardcoded credentials provided by you for the platform master dispatch
    private val masterFonnteToken = "u5WsJdwzLjCq7jnAmaAShmvqFL64AGgwDpxHNgWHyzw4WH"
    private val masterResendApiKey = "re_F2uM6AJ2_3BpjG8qfCBzDEuDq8tgbAWip"

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
        adminEmail: String,
        adminWhatsappNumber: String
    ): Boolean = withContext(Dispatchers.IO) {
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

        // 1. Direct WhatsApp Dispatch (using your official master Fonnte Token)
        if (adminWhatsappNumber.isNotBlank()) {
            try {
                val formBody = FormBody.Builder()
                    .add("target", adminWhatsappNumber)
                    .add("message", formattedMessage)
                    .build()

                val request = Request.Builder()
                    .url("https://api.fonnte.com/send")
                    .addHeader("Authorization", masterFonnteToken)
                    .post(formBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    Log.d("NotificationDispatcher", "WA Fonnte Status: ${response.code} Success: ${response.isSuccessful}")
                }
            } catch (e: Exception) {
                Log.e("NotificationDispatcher", "Direct WA dispatch failed", e)
            }
        }

        // 2. Direct Email Dispatch (using your official master Resend API Key)
        if (adminEmail.isNotBlank()) {
            try {
                val htmlMessage = """
                    <div style="font-family: Arial, sans-serif; padding: 25px; color: #1e293b; background-color: #f8fafc; border-radius: 12px; max-width: 600px; border: 1px solid #e2e8f0;">
                        <h2 style="color: #0f172a; border-bottom: 2px solid #e2e8f0; padding-bottom: 12px; margin-top: 0;">📢 NOTIFIKASI PRESENSI KARYAWAN</h2>
                        <p style="font-size: 15px; color: #475569;">Berikut adalah rincian presensi yang baru saja tercatat di Kiosk:</p>
                        
                        <table style="border-collapse: collapse; width: 100%; margin-top: 15px; background: #ffffff; border-radius: 8px; overflow: hidden;">
                            <tr style="background-color: #f1f5f9;">
                                <td style="padding: 12px; font-weight: bold; width: 30%;">Nama Karyawan</td>
                                <td style="padding: 12px;">${record.employeeName}</td>
                            </tr>
                            <tr>
                                <td style="padding: 12px; font-weight: bold;">Nomor Induk (NIK)</td>
                                <td style="padding: 12px;">${record.employeeCode}</td>
                            </tr>
                            <tr style="background-color: #f1f5f9;">
                                <td style="padding: 12px; font-weight: bold;">Divisi / Dept</td>
                                <td style="padding: 12px;">${record.department}</td>
                            </tr>
                            <tr>
                                <td style="padding: 12px; font-weight: bold;">Tipe Kehadiran</td>
                                <td style="padding: 12px; font-weight: bold; color: #2563eb;">${typeText}</td>
                            </tr>
                            <tr style="background-color: #f1f5f9;">
                                <td style="padding: 12px; font-weight: bold;">Waktu Presensi</td>
                                <td style="padding: 12px;">${record.formattedDate} ${record.formattedTime}</td>
                            </tr>
                            <tr>
                                <td style="padding: 12px; font-weight: bold;">Ketepatan Waktu</td>
                                <td style="padding: 12px; font-weight: bold;">${statusEmoji}</td>
                            </tr>
                            <tr style="background-color: #f1f5f9;">
                                <td style="padding: 12px; font-weight: bold;">Lokasi Presensi</td>
                                <td style="padding: 12px; font-size: 13px; line-height: 1.4;">${record.locationAddress}</td>
                            </tr>
                        </table>
                        
                        <p style="font-size: 11px; color: #94a3b8; text-align: center; margin-top: 25px;">
                            <i>Dikirim secara otomatis oleh SmartPresensi Kiosk AI • Layanan Kehadiran Masa Kini</i>
                        </p>
                    </div>
                """.trimIndent()

                val jsonPayload = """
                    {
                        "from": "onboarding@resend.dev",
                        "to": "$adminEmail",
                        "subject": "🚨 Laporan Presensi Karyawan: ${record.employeeName}",
                        "html": ${escapeJsonString(htmlMessage)}
                    }
                """.trimIndent()

                val body = jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                val request = Request.Builder()
                    .url("https://api.resend.com/emails")
                    .addHeader("Authorization", "Bearer $masterResendApiKey")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    Log.d("NotificationDispatcher", "Email Resend Status: ${response.code} Success: ${response.isSuccessful}")
                }
            } catch (e: Exception) {
                Log.e("NotificationDispatcher", "Direct Email dispatch failed", e)
            }
        }

        true
    }

    private fun escapeJsonString(input: String): String {
        val escaped = input
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }
}
