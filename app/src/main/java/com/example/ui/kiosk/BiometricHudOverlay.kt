package com.example.ui.kiosk

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.biometrics.BiometricScanState
import com.example.biometrics.DetectedGesture
import com.example.data.model.Employee
import com.example.data.model.GestureType
import com.example.ui.theme.AmberCheckout
import com.example.ui.theme.AmberCheckoutLight
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.CyanNeonLight
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.EmeraldSuccessLight
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

    // Laser scan beam animation
    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    // Pulse animation for target reticle
    val reticlePulse by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
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
        // 1. Clean Holographic Biometric Frame & Laser Beam
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height
            val centerX = canvasW / 2f
            val centerY = canvasH * 0.44f
            val reticleW = canvasW * 0.62f * reticlePulse
            val reticleH = canvasH * 0.52f * reticlePulse
            val left = centerX - reticleW / 2f
            val top = centerY - reticleH / 2f
            val right = centerX + reticleW / 2f
            val bottom = centerY + reticleH / 2f

            // Corner Brackets for Face Targeting
            val cornerLen = 32.dp.toPx()
            val strokeWidth = 3.dp.toPx()

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

            // Elliptical Face Silhouette Guide (Thin & Subtle)
            drawOval(
                color = reticleColor.copy(alpha = 0.20f),
                topLeft = Offset(centerX - reticleW * 0.36f, centerY - reticleH * 0.42f),
                size = Size(reticleW * 0.72f, reticleH * 0.84f),
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            )

            // Center Precision Crosshair
            val crosshairSize = 10.dp.toPx()
            drawLine(
                reticleColor.copy(alpha = 0.4f),
                Offset(centerX - crosshairSize, centerY),
                Offset(centerX + crosshairSize, centerY),
                1.dp.toPx()
            )
            drawLine(
                reticleColor.copy(alpha = 0.4f),
                Offset(centerX, centerY - crosshairSize),
                Offset(centerX, centerY + crosshairSize),
                1.dp.toPx()
            )

            // Dynamic Scanning Laser Line
            val currentLaserY = canvasH * scanLineY
            if (currentLaserY in top..bottom) {
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            HudScannerLaser.copy(alpha = 0.7f),
                            HudScannerLaser,
                            HudScannerLaser.copy(alpha = 0.7f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(left, currentLaserY),
                    end = Offset(right, currentLaserY),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        // 2. Gesture Active Status Chip (Only shows when gesture is actively detected)
        if (detectedGesture != null && (detectedGesture.type == GestureType.THUMBS_UP || detectedGesture.type == GestureType.OPEN_HAND)) {
            val isThumbsUp = detectedGesture.type == GestureType.THUMBS_UP
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 18.dp),
                shape = RoundedCornerShape(20.dp),
                color = if (isThumbsUp) EmeraldSuccess.copy(alpha = 0.9f) else AmberCheckout.copy(alpha = 0.9f),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isThumbsUp) Icons.Default.ThumbUp else Icons.Default.PanTool,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isThumbsUp) "MASUK (👍)" else "PULANG (🖐️)",
                        color = Color.Black,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 3. Recognized Employee Identification Badge (Clean Modern Overlay)
        if (recognizedEmployee != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xF00A1124),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, reticleColor)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, reticleColor, CircleShape)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(recognizedEmployee.photoUri.ifBlank { "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100" })
                                .crossfade(true)
                                .build(),
                            contentDescription = recognizedEmployee.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = recognizedEmployee.name,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified",
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = "${recognizedEmployee.role} • ${recognizedEmployee.department}",
                            color = CyanNeonLight,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
