package com.example.biometrics

import android.graphics.PointF
import android.graphics.RectF
import com.example.data.model.GestureType

data class DetectedGesture(
    val type: GestureType,
    val confidence: Float,
    val boundingBox: RectF,
    val landmarks: List<PointF> = emptyList(),
    val label: String
)

data class DetectedFace(
    val boundingBox: RectF,
    val landmarks: List<PointF> = emptyList(),
    val isBlinking: Boolean = false,
    val livenessScore: Float = 0.98f,
    val matchedEmployeeId: Long? = null,
    val matchedEmployeeName: String? = null,
    val confidence: Float = 0.0f
)

enum class BiometricScanState {
    SEARCHING_FACE,       // Mencari Wajah...
    FACE_DETECTED,        // Wajah Terdeteksi, Silakan Berikan Gestur
    VERIFYING_GESTURE,    // Memverifikasi Gestur 👍 / 🖐️
    SUCCESS_CONFIRMED,    // Presensi Berhasil Terverifikasi
    NOT_RECOGNIZED        // Wajah / Gestur Tidak Sesuai
}

class GestureClassifier {

    fun evaluateGesture(
        hasUprightThumb: Boolean,
        hasOpenPalm: Boolean,
        thumbConfidence: Float,
        palmConfidence: Float,
        box: RectF
    ): DetectedGesture? {
        return when {
            hasUprightThumb && thumbConfidence > 0.65f -> {
                DetectedGesture(
                    type = GestureType.THUMBS_UP,
                    confidence = thumbConfidence,
                    boundingBox = box,
                    label = "Jempol / SIP 👍"
                )
            }
            hasOpenPalm && palmConfidence > 0.65f -> {
                DetectedGesture(
                    type = GestureType.OPEN_HAND,
                    confidence = palmConfidence,
                    boundingBox = box,
                    label = "5 Jari Terbuka 🖐️"
                )
            }
            else -> null
        }
    }
}
