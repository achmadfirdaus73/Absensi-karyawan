package com.example.ui.kiosk

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.biometrics.HandGestureDetector
import com.example.data.model.GestureType
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.KioskDarkBackground
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.Executors

private fun isEmulatorEnvironment(): Boolean {
    return (Build.FINGERPRINT.startsWith("generic")
            || Build.FINGERPRINT.startsWith("unknown")
            || Build.MODEL.contains("google_sdk")
            || Build.MODEL.contains("Emulator")
            || Build.MODEL.contains("Android SDK built for x86")
            || Build.HARDWARE.contains("goldfish")
            || Build.HARDWARE.contains("ranchu")
            || Build.PRODUCT.contains("sdk")
            || Build.PRODUCT.contains("google_sdk"))
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreview(
    isFrontCamera: Boolean,
    onVisionEvent: (hasFace: Boolean, gesture: GestureType?) -> Unit,
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

    val faceDetector = remember {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.18f)
            .build()
        FaceDetection.getClient(options)
    }

    val gestureDetector = remember { HandGestureDetector() }

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
            val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
            val previewView = remember {
                PreviewView(context).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
            }

            LaunchedEffect(isFrontCamera, hasCameraPermission) {
                try {
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        try {
                            val provider = cameraProviderFuture.get()
                            cameraProvider = provider
                            provider.unbindAll()

                            val selector = if (isFrontCamera) {
                                if (provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                                    CameraSelector.DEFAULT_FRONT_CAMERA
                                } else {
                                    CameraSelector.DEFAULT_BACK_CAMERA
                                }
                            } else {
                                if (provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                                    CameraSelector.DEFAULT_BACK_CAMERA
                                } else {
                                    CameraSelector.DEFAULT_FRONT_CAMERA
                                }
                            }

                            if (provider.hasCamera(selector)) {
                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }

                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()

                                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null) {
                                        val image = InputImage.fromMediaImage(
                                            mediaImage,
                                            imageProxy.imageInfo.rotationDegrees
                                        )

                                        faceDetector.process(image)
                                            .addOnSuccessListener { faces ->
                                                if (faces.isNotEmpty()) {
                                                    val face = faces[0]
                                                    val detectedGesture = gestureDetector.analyzeFrame(imageProxy, face.boundingBox)
                                                    onVisionEvent(true, detectedGesture)
                                                } else {
                                                    gestureDetector.reset()
                                                    onVisionEvent(false, null)
                                                }
                                            }
                                            .addOnFailureListener {
                                                gestureDetector.reset()
                                                onVisionEvent(false, null)
                                            }
                                            .addOnCompleteListener {
                                                imageProxy.close()
                                            }
                                    } else {
                                        imageProxy.close()
                                    }
                                }

                                provider.bindToLifecycle(lifecycleOwner, selector, preview, imageAnalysis)
                                isHardwareCameraActive = true
                            }
                        } catch (e: Exception) {
                            Log.w("CameraPreview", "Camera initialization issue: ${e.message}")
                            isHardwareCameraActive = false
                        }
                    }, ContextCompat.getMainExecutor(context))
                } catch (e: Exception) {
                    isHardwareCameraActive = false
                }
            }

            DisposableEffect(Unit) {
                onDispose {
                    try {
                        cameraProvider?.unbindAll()
                        cameraExecutor.shutdown()
                    } catch (e: Exception) {
                        Log.e("CameraPreview", "Error unbinding", e)
                    }
                }
            }

            if (isHardwareCameraActive) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Camera Source Badge (Top-Left)
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xCC0B132B),
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
                    text = if (isFrontCamera) "LIVE • Kamera Depan" else "LIVE • Kamera Belakang",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}
