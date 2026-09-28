package com.example.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object DispatchStatusTracker {
    var lastWaResult: String by mutableStateOf("Belum ada pengiriman")
    var lastSheetResult: String by mutableStateOf("Belum ada pengiriman")
}

class AdminNotificationDispatcher(private val context: Context) {
    private val channelId = "admin_attendance_alerts_silent"
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    // Real hardcoded credentials provided by you for the platform master dispatch
    private val masterFonnteToken = "RhmVSsG2fuWMvU4euJoY"

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

    private fun showToastOnMainThread(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    suspend fun dispatchToAdmin(
        record: AttendanceRecord,
        googleSheetUrl: String,
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
                // Ensure correct Indonesian country code prefix if starting with 0 or 08
                var targetNumber = adminWhatsappNumber.trim()
                if (targetNumber.startsWith("0")) {
                    targetNumber = "62" + targetNumber.substring(1)
                }

                val formBody = FormBody.Builder()
                    .add("target", targetNumber)
                    .add("message", formattedMessage)
                    .build()

                val request = Request.Builder()
                    .url("https://api.fonnte.com/send")
                    .addHeader("Authorization", masterFonnteToken.trim())
                    .post(formBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    val bodyString = response.body?.string() ?: ""
                    Log.d("NotificationDispatcher", "WA Fonnte Status: ${response.code} Response: $bodyString")
                    
                    if (response.isSuccessful) {
                        val isTrueStatus = bodyString.contains("\"status\":true") || bodyString.contains("\"status\": true")
                        if (isTrueStatus) {
                            DispatchStatusTracker.lastWaResult = "Sukses (200)"
                            showToastOnMainThread("WhatsApp berhasil dikirim ke $targetNumber!")
                        } else {
                            DispatchStatusTracker.lastWaResult = "Fonnte Menolak: $bodyString"
                            showToastOnMainThread("WA ditolak Fonnte!")
                        }
                    } else {
                        DispatchStatusTracker.lastWaResult = "Gagal HTTP (${response.code}): $bodyString"
                        showToastOnMainThread("Gagal kirim WA (${response.code})")
                    }
                }
            } catch (e: Exception) {
                Log.e("NotificationDispatcher", "Direct WA dispatch failed", e)
                DispatchStatusTracker.lastWaResult = "Error: ${e.localizedMessage}"
                showToastOnMainThread("Koneksi WA Error: ${e.localizedMessage}")
            }
        } else {
            DispatchStatusTracker.lastWaResult = "Gagal: Nomor WA Kosong"
        }

        // 2. Direct Google Sheets Web App Synchronization
        if (googleSheetUrl.isNotBlank() && (googleSheetUrl.startsWith("http://") || googleSheetUrl.startsWith("https://"))) {
            try {
                val cleanLocation = record.locationAddress
                    .replace("\n", " ")
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")

                val jsonPayload = """
                    {
                        "nama": "${record.employeeName}",
                        "nik": "${record.employeeCode}",
                        "divisi": "${record.department}",
                        "waktu": "${record.formattedDate} ${record.formattedTime}",
                        "status": "$typeText",
                        "punctuality": "$statusEmoji",
                        "lokasi": "$cleanLocation"
                    }
                """.trimIndent()

                val body = jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                val request = Request.Builder()
                    .url(googleSheetUrl.trim())
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    val bodyString = response.body?.string() ?: ""
                    Log.d("NotificationDispatcher", "Google Sheet Sync Status: ${response.code} Response: $bodyString")
                    
                    if (response.isSuccessful) {
                        DispatchStatusTracker.lastSheetResult = "Sukses (200)"
                        showToastOnMainThread("Data berhasil sinkron ke Google Sheet!")
                    } else {
                        DispatchStatusTracker.lastSheetResult = "Gagal (${response.code}): $bodyString"
                        showToastOnMainThread("Gagal sinkron Google Sheet (${response.code})")
                    }
                }
            } catch (e: Exception) {
                Log.e("NotificationDispatcher", "Google Sheet sync failed", e)
                DispatchStatusTracker.lastSheetResult = "Error: ${e.localizedMessage}"
                showToastOnMainThread("Koneksi Sheet Error: ${e.localizedMessage}")
            }
        } else {
            DispatchStatusTracker.lastSheetResult = "Belum diisi / Format URL salah"
        }

        true
    }
}
