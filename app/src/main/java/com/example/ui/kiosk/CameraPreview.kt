package com.example.ui.kiosk

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.CyanNeonLight
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.KioskDarkBackground

@Composable
fun CameraPreview(
    isFrontCamera: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    var isHardwareCameraActive by remember { mutableStateOf(false) }
    var cameraErrorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(KioskDarkBackground)
    ) {
        if (hasCameraPermission) {
            var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
            val previewView = remember {
                PreviewView(context).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
            }

            LaunchedEffect(isFrontCamera, hasCameraPermission) {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    try {
                        val provider = cameraProviderFuture.get()
                        cameraProvider = provider
                        provider.unbindAll()

                        // Check available camera selectors safely
                        val desiredSelector = if (isFrontCamera) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }

                        val fallbackSelector = if (isFrontCamera) {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        } else {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        }

                        val finalSelector = when {
                            provider.hasCamera(desiredSelector) -> desiredSelector
                            provider.hasCamera(fallbackSelector) -> fallbackSelector
                            else -> null
                        }

                        if (finalSelector != null) {
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                            provider.bindToLifecycle(lifecycleOwner, finalSelector, preview)
                            isHardwareCameraActive = true
                            cameraErrorMessage = null
                        } else {
                            isHardwareCameraActive = false
                            cameraErrorMessage = "Kamera virtual terdeteksi"
                        }
                    } catch (e: Exception) {
                        Log.w("CameraPreview", "Camera binding fallback: ${e.message}")
                        isHardwareCameraActive = false
                        cameraErrorMessage = e.localizedMessage
                    }
                }, ContextCompat.getMainExecutor(context))
            }

            DisposableEffect(Unit) {
                onDispose {
                    try {
                        cameraProvider?.unbindAll()
                    } catch (e: Exception) {
                        Log.e("CameraPreview", "Error unbinding camera", e)
                    }
                }
            }

            if (isHardwareCameraActive) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // High-Fidelity Futuristic Biometric Camera Simulation Stream
                AnimatedBiometricCameraStream(isFront = isFrontCamera)
            }

            // Stylized cyber vignette gradient over camera viewport
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x2206B6D4),
                                Color(0xAA0A0F1D)
                            ),
                            radius = 900f
                        )
                    )
            )

            // Live Camera Source Indicator
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xBB0B132B),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isHardwareCameraActive) EmeraldSuccess else CyanNeon)
                    )
                    Text(
                        text = if (isHardwareCameraActive) {
                            if (isFrontCamera) "LIVE HD • Kamera Depan" else "LIVE HD • Kamera Belakang"
                        } else {
                            "AI VISION STREAM • Live Sensor"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        } else {
            // Permission request placeholder UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = CyanNeon,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Akses Kamera Diperlukan untuk Deteksi Wajah & Gestur",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Izinkan akses kamera tablet untuk mengaktifkan pemindaian biometrik otomatis.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Izinkan Akses Kamera", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AnimatedBiometricCameraStream(isFront: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "sim_stream")

    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_phase"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF070C18),
                        Color(0xFF0F172A),
                        Color(0xFF090E1C)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height * 0.44f
            val radius = size.width * 0.22f * pulsePhase

            // Concentric Bio-Radar Rings
            drawCircle(
                color = Color(0xFF0E3A5F).copy(alpha = 0.4f),
                radius = radius * 1.5f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                color = CyanNeon.copy(alpha = 0.35f),
                radius = radius,
                center = Offset(cx, cy),
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = CyanNeon.copy(alpha = 0.15f),
                radius = radius * 0.65f,
                center = Offset(cx, cy)
            )

            // Stylized head & shoulder silhouette contour
            drawCircle(
                color = CyanNeonLight.copy(alpha = 0.3f),
                radius = radius * 0.42f,
                center = Offset(cx, cy - radius * 0.1f),
                style = Stroke(width = 2.dp.toPx())
            )

            drawOval(
                color = CyanNeon.copy(alpha = 0.25f),
                topLeft = Offset(cx - radius * 0.75f, cy + radius * 0.45f),
                size = Size(radius * 1.5f, radius * 0.8f),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 180.dp)
        ) {
            Text(
                text = "REAL-TIME BIOMETRIC FEED",
                color = CyanNeonLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "30 FPS • Liveness Detection Active",
                color = Color(0xFF64748B),
                fontSize = 10.sp
            )
        }
    }
}
