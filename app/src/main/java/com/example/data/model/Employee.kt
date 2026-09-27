package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "employees")
data class Employee(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val employeeCode: String, // NIK, e.g., "EMP-1001"
    val name: String,
    val department: String,
    val role: String,
    val email: String,
    val phone: String,
    val shiftStart: String = "08:00",
    val shiftEnd: String = "17:00",
    val photoUri: String = "",
    val faceFeaturesJson: String = "", // Biometric feature signature
    val registeredAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

enum class AttendanceType {
    CHECK_IN,   // Masuk
    CHECK_OUT   // Pulang
}

enum class GestureType {
    THUMBS_UP,  // 👍 Jempol / SIP (Absen Masuk)
    OPEN_HAND,  // 🖐️ Buka 5 Jari (Absen Pulang)
    FACE_ONLY,  // Deteksi Wajah
    MANUAL_PIN  // Manual / PIN Fallback
}

enum class PunctualityStatus {
    ON_TIME,          // Tepat Waktu
    LATE,             // Terlambat
    EARLY_DEPARTURE,  // Pulang Cepat
    OVERTIME,         // Lembur
    NORMAL            // Normal
}
