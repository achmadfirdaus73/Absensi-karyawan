package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class AttendanceRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val employeeDao = db.employeeDao()
    private val attendanceDao = db.attendanceDao()
    private val notificationDao = db.notificationDao()
    private val repoScope = CoroutineScope(Dispatchers.IO)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    val allActiveEmployees: Flow<List<Employee>> = employeeDao.getAllActiveEmployees()
    val allAttendanceRecords: Flow<List<AttendanceRecord>> = attendanceDao.getAllAttendanceRecords()
    val kioskSettingsFlow: Flow<KioskSettings?> = attendanceDao.getKioskSettingsFlow()
    val allNotifications: Flow<List<AdminNotification>> = notificationDao.getAllNotifications()

    suspend fun getKioskSettings(): KioskSettings {
        return withContext(Dispatchers.IO) {
            attendanceDao.getKioskSettings() ?: KioskSettings().also {
                attendanceDao.saveKioskSettings(it)
            }
        }
    }

    suspend fun saveKioskSettings(settings: KioskSettings) {
        withContext(Dispatchers.IO) {
            attendanceDao.saveKioskSettings(settings)
        }
    }

    suspend fun getEmployeeByNikAndPin(nik: String, pin: String): Employee? {
        return withContext(Dispatchers.IO) {
            employeeDao.getEmployeeByNikAndPin(nik, pin)
        }
    }

    suspend fun insertEmployee(employee: Employee): Long {
        return withContext(Dispatchers.IO) {
            employeeDao.insertEmployee(employee)
        }
    }

    suspend fun updateEmployee(employee: Employee) {
        withContext(Dispatchers.IO) {
            employeeDao.updateEmployee(employee)
        }
    }

    suspend fun deleteEmployee(employee: Employee) {
        withContext(Dispatchers.IO) {
            employeeDao.deleteEmployee(employee)
        }
    }

    suspend fun ensureEmployeeEmbeddings(context: Context, employees: List<Employee>) {
        withContext(Dispatchers.IO) {
            for (emp in employees) {
                if (!emp.photoUri.isNullOrBlank()) {
                    val embedding = com.example.util.FaceBiometricHelper.extractEmbeddingFromUri(context, emp.photoUri)
                    if (embedding != null) {
                        val json = com.example.util.FaceBiometricHelper.serializeEmbedding(embedding)
                        if (emp.faceEmbedding != json) {
                            employeeDao.updateEmployee(emp.copy(faceEmbedding = json))
                        }
                    }
                }
            }
        }
    }

    /**
     * Matches live biometric face vector against registered employees' biometrics.
     * Returns the best matching Employee and similarity score (0.0 - 1.0) ONLY if genuine match above threshold.
     */
    fun findBestMatchingEmployee(
        liveBiometricVector: FloatArray,
        employees: List<Employee>,
        threshold: Float = 0.85f
    ): Pair<Employee, Float>? {
        if (employees.isEmpty() || liveBiometricVector.isEmpty()) return null

        var bestMatch: Employee? = null
        var highestScore = 0f

        for (emp in employees) {
            val empEmbedding = com.example.util.FaceBiometricHelper.deserializeEmbedding(emp.faceEmbedding) ?: continue
            val score = com.example.util.FaceBiometricHelper.calculateSimilarity(liveBiometricVector, empEmbedding)

            if (score > highestScore) {
                highestScore = score
                bestMatch = emp
            }
        }

        // Required threshold: strictly require real matching (default min 0.85)
        val minAcceptableThreshold = if (threshold > 0f) threshold else 0.85f
        return if (bestMatch != null && highestScore >= minAcceptableThreshold) {
            Pair(bestMatch, highestScore)
        } else {
            null
        }
    }

    suspend fun deleteAttendance(id: Long) {
        withContext(Dispatchers.IO) {
            attendanceDao.deleteAttendance(id)
        }
    }

    data class ShiftResolution(
        val shiftName: String,
        val startTime: String,
        val endTime: String
    )

    /**
     * Resolves the active work shift for an employee based on their configured shift type.
     */
    fun resolveShiftForEmployee(
        employee: Employee,
        settings: KioskSettings,
        calendar: Calendar = Calendar.getInstance()
    ): ShiftResolution {
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

        return when (employee.shiftType) {
            ShiftAssignmentType.SHIFT_1.name -> {
                ShiftResolution("Shift 1 (Pagi)", settings.shift1Start, settings.shift1End)
            }
            ShiftAssignmentType.SHIFT_2.name -> {
                ShiftResolution("Shift 2 (Siang/Sore)", settings.shift2Start, settings.shift2End)
            }
            ShiftAssignmentType.SHIFT_3.name -> {
                ShiftResolution("Shift 3 (Malam)", settings.shift3Start, settings.shift3End)
            }
            ShiftAssignmentType.MANUAL_CUSTOM.name -> {
                val start = if (employee.customStartTime.isNotBlank()) employee.customStartTime else "08:00"
                val end = if (employee.customEndTime.isNotBlank()) employee.customEndTime else "17:00"
                ShiftResolution("Khusus (${employee.role}): $start - $end", start, end)
            }
            else -> {
                // AUTO_DETECT: Pick the closest shift start
                val s1StartMin = parseTimeToMinutes(settings.shift1Start)
                val s2StartMin = parseTimeToMinutes(settings.shift2Start)
                val s3StartMin = parseTimeToMinutes(settings.shift3Start)

                val diff1 = circularMinuteDiff(currentMinutes, s1StartMin)
                val diff2 = circularMinuteDiff(currentMinutes, s2StartMin)
                val diff3 = circularMinuteDiff(currentMinutes, s3StartMin)

                when {
                    diff1 <= diff2 && diff1 <= diff3 -> {
                        ShiftResolution("Shift 1 (Pagi)", settings.shift1Start, settings.shift1End)
                    }
                    diff2 <= diff1 && diff2 <= diff3 -> {
                        ShiftResolution("Shift 2 (Siang)", settings.shift2Start, settings.shift2End)
                    }
                    else -> {
                        ShiftResolution("Shift 3 (Malam)", settings.shift3Start, settings.shift3End)
                    }
                }
            }
        }
    }

    /**
     * Process biometric or PIN attendance check
     */
    suspend fun processAttendance(
        employee: Employee,
        checkType: CheckType,
        confidenceScore: Float = 0.95f,
        photoUri: String? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ): AttendanceRecord {
        return withContext(Dispatchers.IO) {
            val settings = getKioskSettings()
            val now = Calendar.getInstance()
            val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

            val resolvedShift = resolveShiftForEmployee(employee, settings, now)
            val shiftStartMin = parseTimeToMinutes(resolvedShift.startTime)
            val shiftEndMin = parseTimeToMinutes(resolvedShift.endTime)

            val status: AttendanceStatus = if (checkType == CheckType.CHECK_IN) {
                // If checking in after shift start + 15 minutes grace period
                val lateDiff = (currentMinutes - shiftStartMin + 1440) % 1440
                if (lateDiff in 16..480) {
                    AttendanceStatus.LATE
                } else {
                    AttendanceStatus.ON_TIME
                }
            } else {
                // CHECK_OUT
                val earlyDiff = (shiftEndMin - currentMinutes + 1440) % 1440
                val overtimeDiff = (currentMinutes - shiftEndMin + 1440) % 1440
                if (earlyDiff in 16..360) {
                    AttendanceStatus.EARLY_DEPARTURE
                } else if (overtimeDiff in 31..480) {
                    AttendanceStatus.OVERTIME
                } else {
                    AttendanceStatus.ON_TIME
                }
            }

            val record = AttendanceRecord(
                employeeId = employee.id,
                employeeName = employee.name,
                nik = employee.nik,
                role = employee.role,
                shiftName = resolvedShift.shiftName,
                timestamp = System.currentTimeMillis(),
                checkType = checkType,
                status = status,
                confidenceScore = confidenceScore,
                photoUri = photoUri,
                latitude = latitude,
                longitude = longitude
            )

            val insertedId = attendanceDao.insertAttendance(record)
            val savedRecord = record.copy(id = insertedId)

            // Log Admin Notification
            val notifTitle = if (checkType == CheckType.CHECK_IN) "Absen Masuk: ${employee.name}" else "Absen Pulang: ${employee.name}"
            val statusLabel = when (status) {
                AttendanceStatus.ON_TIME -> "Tepat Waktu"
                AttendanceStatus.LATE -> "Terlambat"
                AttendanceStatus.EARLY_DEPARTURE -> "Pulang Awal"
                AttendanceStatus.OVERTIME -> "Lembur"
            }
            notificationDao.insertNotification(
                AdminNotification(
                    title = notifTitle,
                    message = "${employee.name} (${employee.role} - ${employee.division}) berhasil ${if (checkType == CheckType.CHECK_IN) "Masuk" else "Pulang"} [$statusLabel] pada ${resolvedShift.shiftName}",
                    type = if (status == AttendanceStatus.LATE || status == AttendanceStatus.EARLY_DEPARTURE) "ALERT" else "SUCCESS"
                )
            )

            // Asynchronously dispatch cloud sync (WA & Sheets)
            dispatchCloudSync(savedRecord, employee, settings)

            savedRecord
        }
    }

    private fun dispatchCloudSync(record: AttendanceRecord, employee: Employee, settings: KioskSettings) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val timeStr = dateFormat.format(Date(record.timestamp))
        val typeStr = if (record.checkType == CheckType.CHECK_IN) "ABSEN MASUK" else "ABSEN PULANG"
        val statusStr = when (record.status) {
            AttendanceStatus.ON_TIME -> "✅ Tepat Waktu"
            AttendanceStatus.LATE -> "⚠️ Terlambat"
            AttendanceStatus.EARLY_DEPARTURE -> "⏳ Pulang Awal"
            AttendanceStatus.OVERTIME -> "🔥 Lembur"
        }

        // 1. WhatsApp Fonnte Dispatch
        if (settings.fonnteApiKey.isNotBlank() && settings.fonnteAdminWa.isNotBlank()) {
            try {
                val waMessage = """
                    📢 *NOTIFIKASI PRESENSI KIOSK*
                    ----------------------------------
                    👤 *Nama:* ${employee.name}
                    🆔 *NIK:* ${employee.nik}
                    💼 *Jabatan / Role:* ${employee.role}
                    🏢 *Divisi:* ${employee.division}
                    ⏰ *Tipe:* $typeStr
                    🕒 *Waktu:* $timeStr
                    📋 *Shift:* ${record.shiftName}
                    📊 *Status:* $statusStr
                    ----------------------------------
                    🏢 *${settings.companyName}*
                """.trimIndent()

                val jsonBody = JSONObject().apply {
                    put("target", settings.fonnteAdminWa)
                    put("message", waMessage)
                }

                val request = Request.Builder()
                    .url("https://api.fonnte.com/send")
                    .addHeader("Authorization", settings.fonnteApiKey)
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                    .build()

                httpClient.newCall(request).execute().close()
            } catch (_: Exception) {
                // Ignore network errors in background
            }
        }

        // 2. Google Sheets Webhook Dispatch
        if (settings.googleSheetsUrl.isNotBlank()) {
            try {
                val sheetJson = JSONObject().apply {
                    put("timestamp", timeStr)
                    put("nik", employee.nik)
                    put("name", employee.name)
                    put("division", employee.division)
                    put("role", employee.role)
                    put("type", if (record.checkType == CheckType.CHECK_IN) "Masuk" else "Pulang")
                    put("shift", record.shiftName)
                    put("status", record.status.name)
                    put("confidence", "${(record.confidenceScore * 100).toInt()}%")
                }

                val request = Request.Builder()
                    .url(settings.googleSheetsUrl)
                    .post(sheetJson.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                    .build()

                val response = httpClient.newCall(request).execute()
                val isSuccess = response.isSuccessful
                response.close()

                // Update settings sync status
                val newStatus = if (isSuccess) "Tersinkron: $timeStr" else "Gagal kirim: HTTP ${response.code}"
                repoScope.launch {
                    attendanceDao.saveKioskSettings(settings.copy(lastSheetsSyncStatus = newStatus))
                }
            } catch (e: Exception) {
                val errStatus = "Gagal kirim: ${e.localizedMessage ?: "Error koneksi"}"
                repoScope.launch {
                    attendanceDao.saveKioskSettings(settings.copy(lastSheetsSyncStatus = errStatus))
                }
            }
        }
    }

    suspend fun testSendWhatsApp(apiKey: String, targetPhone: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                if (apiKey.isBlank() || targetPhone.isBlank()) {
                    return@withContext Result.failure(Exception("API Token Fonnte dan Nomor WhatsApp harus diisi."))
                }

                val waMessage = """
                    ✅ *TES KONEKSI WHATSAPP FONNTE BERHASIL!*
                    ----------------------------------
                    Aplikasi Smart Presensi Kiosk berhasil terhubung dengan API Fonnte.
                    Waktu: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}
                """.trimIndent()

                val jsonBody = JSONObject().apply {
                    put("target", targetPhone)
                    put("message", waMessage)
                }

                val request = Request.Builder()
                    .url("https://api.fonnte.com/send")
                    .addHeader("Authorization", apiKey.trim())
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""
                response.close()

                if (response.isSuccessful) {
                    Result.success("Pesan WhatsApp berhasil dikirim ke $targetPhone! Response: $responseBody")
                } else {
                    Result.failure(Exception("Gagal mengirim (HTTP ${response.code}): $responseBody"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun parseTimeToMinutes(timeStr: String): Int {
        return try {
            val parts = timeStr.trim().split(":")
            val h = parts[0].toInt()
            val m = if (parts.size > 1) parts[1].toInt() else 0
            h * 60 + m
        } catch (_: Exception) {
            480 // 08:00 default
        }
    }

    private fun circularMinuteDiff(timeA: Int, timeB: Int): Int {
        val diff = Math.abs(timeA - timeB)
        return Math.min(diff, 1440 - diff)
    }
}
