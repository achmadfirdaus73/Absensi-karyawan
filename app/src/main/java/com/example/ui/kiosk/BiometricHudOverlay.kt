package com.example.ui.kiosk

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biometrics.BiometricScanState
import com.example.biometrics.DetectedGesture
import com.example.data.model.Employee
import com.example.data.model.GestureType
import com.example.ui.theme.AmberCheckout
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.CyanNeonLight
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.HudReticleDetecting
import com.example.ui.theme.HudReticleNormal
import com.example.ui.theme.HudReticleSuccess
import com.example.ui.theme.HudScannerLaser

@Composable
fun BiometricHudOverlay(
    scanState: BiometricScanState,
    recognizedEmployee: Employee?,
    detectedGesture: DetectedGesture?,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hud_anim")

    // Laser scan animation
    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    // Pulse animation for reticle
    val reticlePulse by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "reticle_pulse"
    )

    val reticleColor = when (scanState) {
        BiometricScanState.SEARCHING_FACE -> HudReticleNormal
        BiometricScanState.FACE_DETECTED -> HudReticleDetecting
        BiometricScanState.VERIFYING_GESTURE -> CyanNeon
        BiometricScanState.SUCCESS_CONFIRMED -> HudReticleSuccess
        BiometricScanState.NOT_RECOGNIZED -> Color(0xFFF43F5E)
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. HUD Canvas Drawing (Biometric Grid, Scanner Beam, Reticle Brackets)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height
            val centerX = canvasW / 2f
            val centerY = canvasH * 0.45f
            val reticleW = canvasW * 0.55f * reticlePulse
            val reticleH = canvasH * 0.48f * reticlePulse
            val left = centerX - reticleW / 2f
            val top = centerY - reticleH / 2f
            val right = centerX + reticleW / 2f
            val bottom = centerY + reticleH / 2f

            // Corner Brackets for Face Frame
            val cornerLen = 40.dp.toPx()
            val strokeWidth = 3.5.dp.toPx()

            // Top-Left Corner
            drawLine(reticleColor, Offset(left, top), Offset(left + cornerLen, top), strokeWidth)
            drawLine(reticleColor, Offset(left, top), Offset(left, top + cornerLen), strokeWidth)

            // Top-Right Corner
            drawLine(reticleColor, Offset(right, top), Offset(right - cornerLen, top), strokeWidth)
            drawLine(reticleColor, Offset(right, top), Offset(right, top + cornerLen), strokeWidth)

            // Bottom-Left Corner
            drawLine(reticleColor, Offset(left, bottom), Offset(left + cornerLen, bottom), strokeWidth)
            drawLine(reticleColor, Offset(left, bottom), Offset(left, bottom - cornerLen), strokeWidth)

            // Bottom-Right Corner
            drawLine(reticleColor, Offset(right, bottom), Offset(right - cornerLen, bottom), strokeWidth)
            drawLine(reticleColor, Offset(right, bottom), Offset(right, bottom - cornerLen), strokeWidth)

            // Elliptical Face Alignment Guide
            drawOval(
                color = reticleColor.copy(alpha = 0.25f),
                topLeft = Offset(centerX - reticleW * 0.38f, centerY - reticleH * 0.44f),
                size = Size(reticleW * 0.76f, reticleH * 0.88f),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                )
            )

            // Center Crosshair
            val crosshairSize = 14.dp.toPx()
            drawLine(
                reticleColor.copy(alpha = 0.5f),
                Offset(centerX - crosshairSize, centerY),
                Offset(centerX + crosshairSize, centerY),
                1.5.dp.toPx()
            )
            drawLine(
                reticleColor.copy(alpha = 0.5f),
                Offset(centerX, centerY - crosshairSize),
                Offset(centerX, centerY + crosshairSize),
                1.5.dp.toPx()
            )

            // Moving Laser Scanning Beam
            val currentLaserY = canvasH * scanLineY
            if (currentLaserY in top..bottom) {
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            HudScannerLaser.copy(alpha = 0.8f),
                            HudScannerLaser,
                            HudScannerLaser.copy(alpha = 0.8f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(left, currentLaserY),
                    end = Offset(right, currentLaserY),
                    strokeWidth = 2.5.dp.toPx()
                )

                // Laser Glow gradient area
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            HudScannerLaser.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        startY = currentLaserY,
                        endY = currentLaserY + 25.dp.toPx()
                    ),
                    topLeft = Offset(left, currentLaserY),
                    size = Size(reticleW, 25.dp.toPx())
                )
            }
        }

        // 2. Gesture Detection Floating Guide Bar at top of HUD
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xDD0F172A),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Thumbs up indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (detectedGesture?.type == GestureType.THUMBS_UP)
                                EmeraldSuccess.copy(alpha = 0.3f)
                            else
                                Color.Transparent
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = "Jempol Masuk",
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Jempol 👍 : MASUK",
                        color = if (detectedGesture?.type == GestureType.THUMBS_UP) EmeraldSuccess else Color(0xFFE2E8F0),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(16.dp)
                        .background(Color(0xFF334155))
                )

                // 5 fingers indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (detectedGesture?.type == GestureType.OPEN_HAND)
                                AmberCheckout.copy(alpha = 0.3f)
                            else
                                Color.Transparent
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PanTool,
                        contentDescription = "5 Jari Pulang",
                        tint = AmberCheckout,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "5 Jari 🖐️ : PULANG",
                        color = if (detectedGesture?.type == GestureType.OPEN_HAND) AmberCheckout else Color(0xFFE2E8F0),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 3. Biometric Target Status Tag (Face Identification feedback)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xEE0B132B),
            border = androidx.compose.foundation.BorderStroke(1.dp, reticleColor.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(reticleColor)
                )

                Column {
                    val title = when (scanState) {
                        BiometricScanState.SEARCHING_FACE -> "AI Scanning • Deteksi Wajah Aktif"
                        BiometricScanState.FACE_DETECTED -> "Wajah Terdeteksi • Tunjukkan Gestur 👍 atau 🖐️"
                        BiometricScanState.VERIFYING_GESTURE -> "Memverifikasi: ${recognizedEmployee?.name ?: "Karyawan"}"
                        BiometricScanState.SUCCESS_CONFIRMED -> "Verifikasi Sukses • Cloud Synced"
                        BiometricScanState.NOT_RECOGNIZED -> "Wajah Tidak Terdaftar"
                    }
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Akurasi Biometrik: 98.6% • Liveness Active",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
