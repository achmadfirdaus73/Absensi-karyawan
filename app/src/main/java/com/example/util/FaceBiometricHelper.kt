package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.tasks.Tasks
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sqrt

object FaceBiometricHelper {
    private val gson = Gson()
    private const val TAG = "FaceBiometricHelper"

    // Detector options with full landmarks and contours for maximum biometric accuracy
    val biometricDetectorOptions: FaceDetectorOptions by lazy {
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.12f)
            .enableTracking()
            .build()
    }

    /**
     * Extracts biometric feature vector from an ML Kit Face object and optional cropped face bitmap.
     */
    fun extractFeatureVector(face: Face, faceCropBitmap: Bitmap? = null): FloatArray {
        val features = FloatArray(64)
        val box = face.boundingBox
        val boxWidth = box.width().toFloat().coerceAtLeast(1f)
        val boxHeight = box.height().toFloat().coerceAtLeast(1f)
        val boxLeft = box.left.toFloat()
        val boxTop = box.top.toFloat()

        // 1. Aspect ratio
        features[0] = boxWidth / boxHeight

        // 2. Key Landmarks (normalized to [0..1] within bounding box)
        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position
        val noseBase = face.getLandmark(FaceLandmark.NOSE_BASE)?.position
        val mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position
        val mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position
        val mouthBottom = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position
        val leftCheek = face.getLandmark(FaceLandmark.LEFT_CHEEK)?.position
        val rightCheek = face.getLandmark(FaceLandmark.RIGHT_CHEEK)?.position

        var eyeDist = 1f
        if (leftEye != null && rightEye != null) {
            features[1] = (leftEye.x - boxLeft) / boxWidth
            features[2] = (leftEye.y - boxTop) / boxHeight
            features[3] = (rightEye.x - boxLeft) / boxWidth
            features[4] = (rightEye.y - boxTop) / boxHeight

            val dx = leftEye.x - rightEye.x
            val dy = leftEye.y - rightEye.y
            eyeDist = hypot(dx, dy).coerceAtLeast(1f)
            features[5] = eyeDist / boxWidth
        }

        if (noseBase != null) {
            features[6] = (noseBase.x - boxLeft) / boxWidth
            features[7] = (noseBase.y - boxTop) / boxHeight

            if (leftEye != null && rightEye != null) {
                val eyeMidX = (leftEye.x + rightEye.x) / 2f
                val eyeMidY = (leftEye.y + rightEye.y) / 2f
                val eyeToNose = hypot(noseBase.x - eyeMidX, noseBase.y - eyeMidY)
                features[8] = eyeToNose / eyeDist
            }
        }

        if (mouthLeft != null && mouthRight != null) {
            features[9] = (mouthLeft.x - boxLeft) / boxWidth
            features[10] = (mouthLeft.y - boxTop) / boxHeight
            features[11] = (mouthRight.x - boxLeft) / boxWidth
            features[12] = (mouthRight.y - boxTop) / boxHeight

            val mouthWidth = hypot(mouthLeft.x - mouthRight.x, mouthLeft.y - mouthRight.y)
            features[13] = mouthWidth / eyeDist

            if (noseBase != null) {
                val mouthMidX = (mouthLeft.x + mouthRight.x) / 2f
                val mouthMidY = (mouthLeft.y + mouthRight.y) / 2f
                val noseToMouth = hypot(mouthMidX - noseBase.x, mouthMidY - noseBase.y)
                features[14] = noseToMouth / eyeDist
            }
        }

        if (mouthBottom != null) {
            features[15] = (mouthBottom.x - boxLeft) / boxWidth
            features[16] = (mouthBottom.y - boxTop) / boxHeight
        }

        if (leftCheek != null && rightCheek != null) {
            features[17] = (leftCheek.x - boxLeft) / boxWidth
            features[18] = (leftCheek.y - boxTop) / boxHeight
            features[19] = (rightCheek.x - boxLeft) / boxWidth
            features[20] = (rightCheek.y - boxTop) / boxHeight
            val cheekDist = hypot(leftCheek.x - rightCheek.x, leftCheek.y - rightCheek.y)
            features[21] = cheekDist / boxWidth
        }

        // 3. Face Contours (Sample critical boundary points)
        val faceOval = face.getContour(FaceContour.FACE)?.points
        if (!faceOval.isNullOrEmpty()) {
            val step = (faceOval.size / 8).coerceAtLeast(1)
            var idx = 22
            for (i in 0 until 8) {
                val pointIndex = (i * step).coerceAtMost(faceOval.size - 1)
                val pt = faceOval[pointIndex]
                if (idx < 38) {
                    features[idx++] = (pt.x - boxLeft) / boxWidth
                    features[idx++] = (pt.y - boxTop) / boxHeight
                }
            }
        }

        // 4. Downsampled 4x4 grayscale appearance histogram if crop bitmap is provided
        if (faceCropBitmap != null && !faceCropBitmap.isRecycled) {
            try {
                val small = Bitmap.createScaledBitmap(faceCropBitmap, 4, 4, true)
                var idx = 38
                for (y in 0 until 4) {
                    for (x in 0 until 4) {
                        if (idx < 54) {
                            val pixel = small.getPixel(x, y)
                            val r = (pixel shr 16) and 0xFF
                            val g = (pixel shr 8) and 0xFF
                            val b = pixel and 0xFF
                            // Normalized luminance [0..1]
                            val lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
                            features[idx++] = lum
                        }
                    }
                }
                small.recycle()
            } catch (e: Exception) {
                Log.w(TAG, "Failed extracting appearance histogram", e)
            }
        }

        // 5. Normalization (L2 norm)
        var sumSquares = 0f
        for (v in features) {
            sumSquares += v * v
        }
        val norm = sqrt(sumSquares.coerceAtLeast(1e-6f))
        for (i in features.indices) {
            features[i] /= norm
        }

        return features
    }

    /**
     * Extracts embedding from a photo file/Uri. Runs on background thread.
     */
    suspend fun extractEmbeddingFromUri(context: Context, photoUriStr: String): FloatArray? {
        return withContext(Dispatchers.IO) {
            try {
                val bitmap = loadAndCorrectBitmap(context, photoUriStr) ?: return@withContext null
                val image = InputImage.fromBitmap(bitmap, 0)
                val detector = FaceDetection.getClient(biometricDetectorOptions)
                val task = detector.process(image)
                val faces: List<Face> = Tasks.await(task)
                detector.close()

                if (faces.isNotEmpty()) {
                    val primaryFace = faces.maxByOrNull { f -> f.boundingBox.width() * f.boundingBox.height() } ?: faces.first()
                    // Crop face area
                    val box = primaryFace.boundingBox
                    val safeLeft = box.left.coerceIn(0, bitmap.width - 1)
                    val safeTop = box.top.coerceIn(0, bitmap.height - 1)
                    val safeWidth = box.width().coerceIn(1, bitmap.width - safeLeft)
                    val safeHeight = box.height().coerceIn(1, bitmap.height - safeTop)
                    
                    val crop = try {
                        Bitmap.createBitmap(bitmap, safeLeft, safeTop, safeWidth, safeHeight)
                    } catch (_: Exception) {
                        null
                    }

                    val vector = extractFeatureVector(primaryFace, crop)
                    crop?.recycle()
                    bitmap.recycle()
                    vector
                } else {
                    bitmap.recycle()
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error extracting embedding from uri: $photoUriStr", e)
                null
            }
        }
    }

    /**
     * Compares two biometric feature vectors and returns similarity percentage (0.0 to 1.0).
     */
    fun calculateSimilarity(v1: FloatArray, v2: FloatArray): Float {
        if (v1.isEmpty() || v2.isEmpty()) return 0f
        val len = minOf(v1.size, v2.size)
        var dot = 0f
        var norm1 = 0f
        var norm2 = 0f
        var euclideanDist = 0f

        for (i in 0 until len) {
            val a = v1[i]
            val b = v2[i]
            dot += a * b
            norm1 += a * a
            norm2 += b * b
            val diff = a - b
            euclideanDist += diff * diff
        }

        val cosineSim = if (norm1 > 0f && norm2 > 0f) {
            (dot / (sqrt(norm1) * sqrt(norm2))).coerceIn(-1f, 1f)
        } else {
            0f
        }

        val euclideanSim = (1f - (sqrt(euclideanDist) / 2f)).coerceIn(0f, 1f)
        
        // Weighted composite score
        val score = (cosineSim * 0.70f + euclideanSim * 0.30f).coerceIn(0f, 1f)
        return score
    }

    fun serializeEmbedding(features: FloatArray): String {
        return gson.toJson(features)
    }

    fun deserializeEmbedding(json: String?): FloatArray? {
        if (json.isNullOrBlank()) return null
        return try {
            val type = object : TypeToken<FloatArray>() {}.type
            gson.fromJson<FloatArray>(json, type)
        } catch (_: Exception) {
            null
        }
    }

    private fun loadAndCorrectBitmap(context: Context, pathOrUri: String): Bitmap? {
        return try {
            var inputStream: InputStream? = null
            var orientation = ExifInterface.ORIENTATION_NORMAL

            val bitmap = if (pathOrUri.startsWith("content://") || pathOrUri.startsWith("file://")) {
                val uri = Uri.parse(pathOrUri)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                }
                inputStream = context.contentResolver.openInputStream(uri)
                BitmapFactory.decodeStream(inputStream)
            } else {
                val file = File(pathOrUri)
                if (!file.exists()) return null
                val exif = ExifInterface(file.absolutePath)
                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                BitmapFactory.decodeFile(file.absolutePath)
            }
            inputStream?.close()

            if (bitmap == null) return null

            // Apply rotation if needed
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            }

            if (matrix.isIdentity) {
                bitmap
            } else {
                val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                bitmap.recycle()
                rotated
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed loading bitmap: $pathOrUri", e)
            null
        }
    }
}
