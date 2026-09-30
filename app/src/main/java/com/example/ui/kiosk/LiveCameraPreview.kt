package com.example.ui.kiosk

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.util.FaceBiometricHelper
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@Composable
fun LiveCameraPreview(
    modifier: Modifier = Modifier,
    useFrontCamera: Boolean = true,
    onFacesDetected: (List<Face>) -> Unit = {},
    onFaceBiometrics: (Face, FloatArray) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraError by remember { mutableStateOf(false) }

    // Setup high-performance Accurate Real-Time ML Kit Face Detector
    val faceDetector: FaceDetector = remember {
        FaceDetection.getClient(FaceBiometricHelper.biometricDetectorOptions)
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            faceDetector.close()
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        
                        // 1. Camera Preview Stream
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        // 2. Real-Time Face Recognition & Analysis Stream
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy: ImageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                faceDetector.process(image)
                                    .addOnSuccessListener { faces ->
                                        onFacesDetected(faces)
                                        if (faces.isNotEmpty()) {
                                            val primaryFace = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() } ?: faces.first()
                                            val vector = FaceBiometricHelper.extractFeatureVector(primaryFace)
                                            onFaceBiometrics(primaryFace, vector)
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("LiveCameraPreview", "ML Kit Face detection error", e)
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }

                        val cameraSelector = if (useFrontCamera && cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }

                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (exc: Exception) {
                        Log.e("LiveCameraPreview", "Camera bind failed", exc)
                        hasCameraError = true
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        if (hasCameraError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Mode Kamera Kiosk Aktif",
                    color = Color(0xFF00E5FF)
                )
            }
        }
    }
}
