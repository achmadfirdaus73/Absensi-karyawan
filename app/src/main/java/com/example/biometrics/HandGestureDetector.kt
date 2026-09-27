package com.example.biometrics

import android.graphics.Rect
import androidx.camera.core.ImageProxy
import com.example.data.model.GestureType

class HandGestureDetector {

    private var consecutiveThumbFrames = 0
    private var consecutivePalmFrames = 0
    private val REQUIRED_STABLE_FRAMES = 6 // ~0.8 to 1.0 seconds of steady hold

    fun analyzeFrame(
        imageProxy: ImageProxy,
        faceBounds: Rect
    ): GestureType? {
        try {
            val planes = imageProxy.planes
            if (planes.isEmpty()) return null

            val buffer = planes[0].buffer
            val width = imageProxy.width
            val height = imageProxy.height
            val rowStride = planes[0].rowStride
            val pixelStride = planes[0].pixelStride

            // Region of Interest: Look at the side of the face (left/right quadrants where hand is raised)
            // Hand is usually placed on the left or right of the face, around mid-face height
            val faceWidth = faceBounds.width().coerceAtLeast(80)
            val faceHeight = faceBounds.height().coerceAtLeast(80)

            // Calculate hand search box (to the right of the face, or below chin)
            val handLeft = (faceBounds.right).coerceIn(0, width - 1)
            val handRight = (faceBounds.right + faceWidth).coerceIn(0, width)
            val handTop = (faceBounds.top).coerceIn(0, height - 1)
            val handBottom = (faceBounds.bottom + faceHeight / 2).coerceIn(0, height)

            if (handRight <= handLeft || handBottom <= handTop) {
                reset()
                return null
            }

            var skinPixelCount = 0
            var totalSampled = 0
            var verticalVariance = 0
            var horizontalVariance = 0

            val step = 4
            var minX = width
            var maxX = 0
            var minY = height
            var maxY = 0

            val dataSize = buffer.remaining()
            val byteArray = ByteArray(dataSize.coerceAtMost(8192))
            buffer.get(byteArray)

            for (y in handTop until handBottom step step) {
                for (x in handLeft until handRight step step) {
                    val index = (y * width / height * 8 + x) % byteArray.size
                    if (index in byteArray.indices) {
                        val lum = byteArray[index].toInt() and 0xFF
                        totalSampled++
                        // Skin tone / foreground reflection threshold
                        if (lum in 80..220) {
                            skinPixelCount++
                            if (x < minX) minX = x
                            if (x > maxX) maxX = x
                            if (y < minY) minY = y
                            if (y > maxY) maxY = y
                        }
                    }
                }
            }

            val density = if (totalSampled > 0) skinPixelCount.toFloat() / totalSampled else 0f
            val handW = (maxX - minX).coerceAtLeast(1)
            val handH = (maxY - minY).coerceAtLeast(1)
            val aspectRatio = handW.toFloat() / handH.toFloat()

            // Heuristic differentiation:
            // 1. Thumb Up: compact cluster, taller than wide (aspect ratio < 0.85), moderate density
            // 2. Open Palm (5 fingers): wider cluster (aspect ratio >= 0.85), high density
            if (density > 0.35f && handH > 30) {
                if (aspectRatio < 0.85f) {
                    consecutiveThumbFrames++
                    consecutivePalmFrames = 0
                    if (consecutiveThumbFrames >= REQUIRED_STABLE_FRAMES) {
                        return GestureType.THUMBS_UP
                    }
                } else {
                    consecutivePalmFrames++
                    consecutiveThumbFrames = 0
                    if (consecutivePalmFrames >= REQUIRED_STABLE_FRAMES) {
                        return GestureType.OPEN_HAND
                    }
                }
            } else {
                reset()
            }
        } catch (e: Exception) {
            reset()
        }
        return null
    }

    fun reset() {
        consecutiveThumbFrames = 0
        consecutivePalmFrames = 0
    }
}
