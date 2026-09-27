package com.example.ui.kiosk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceType
import com.example.data.model.GestureType
import com.example.network.CloudSyncStatus
import com.example.ui.theme.AmberCheckout
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.CyanNeonLight
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.KioskCardBorder
import com.example.ui.theme.KioskDarkBackground
import com.example.ui.theme.KioskDarkSurface
import com.example.ui.viewmodel.AttendanceViewModel

@Composable
fun KioskScreen(
    viewModel: AttendanceViewModel,
    onOpenAdmin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activeEmployees by viewModel.activeEmployees.collectAsStateWithLifecycle()
    val recentAttendance by viewModel.recentAttendance.collectAsStateWithLifecycle()
    val kioskSettings by viewModel.kioskSettings.collectAsStateWithLifecycle()

    val currentEmployee = uiState.recognizedEmployee

    // Trigger attendance ONLY when:
    // 1. Employee face is recognized
    // 2. An actual gesture is detected (👍 or 🖐️)
    LaunchedEffect(uiState.detectedGesture, currentEmployee, uiState.isProcessingAttendance, uiState.showSuccessDialog) {
        val gesture = uiState.detectedGesture
        if (currentEmployee != null && gesture != null && !uiState.isProcessingAttendance && !uiState.showSuccessDialog) {
            if (gesture.type == GestureType.THUMBS_UP || gesture.type == GestureType.OPEN_HAND) {
                viewModel.processBiometricAttendance(currentEmployee, gesture.type)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(KioskDarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. MINIMALIST TOP STATUS BAR
            MinimalKioskHeader(
                companyName = kioskSettings?.companyName ?: "PT DIGITAL NUSANTARA UTAMA",
                timeString = uiState.currentTimeString,
                dateString = uiState.currentDateString,
                locationAddress = uiState.locationInfo.address,
                isGeofenceOk = uiState.locationInfo.isWithinOfficeGeofence,
                cloudStatus = uiState.cloudStatus,
                onOpenAdmin = onOpenAdmin
            )

            // 2. MAIN FULLSTAGE CAMERA VIEWPORT (ZERO BUTTONS - PURE GESTURE ATTENDANCE)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.5.dp, KioskCardBorder, RoundedCornerShape(24.dp))
            ) {
                // Live Hardware Camera with HandGestureDetector
                CameraPreview(
                    isFrontCamera = uiState.isFrontCamera,
                    onVisionEvent = { hasFace, gesture ->
                        viewModel.onLiveCameraVisionDetected(hasFace, gesture)
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // ONLY the Detected Employee Badge appears when face is recognized (NO BUTTONS)
                BiometricHudOverlay(
                    recognizedEmployee = currentEmployee,
                    activeGesture = uiState.detectedGesture?.type,
                    countdownProgress = uiState.holdCountdownSec
                )

                // Subtle Camera Switcher Button (Top-Right)
                IconButton(
                    onClick = { viewModel.toggleCamera() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(Color(0x990F172A), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.FlipCameraAndroid,
                        contentDescription = "Ganti Kamera",
                        tint = CyanNeon,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 3. MINIMALIST REAL-TIME ATTENDANCE TICKER (BOTTOM LOG)
            MinimalAttendanceTicker(
                recentAttendance = recentAttendance,
                hasEnrolledEmployees = activeEmployees.isNotEmpty(),
                hasRecognizedFace = currentEmployee != null
            )
        }

        // 4. INSTANT VERIFIED ATTENDANCE POPUP OVERLAY
        if (uiState.showSuccessDialog && uiState.lastVerifiedRecord != null) {
            AttendanceSuccessDialog(
                record = uiState.lastVerifiedRecord!!,
                onDismiss = { viewModel.dismissSuccessDialog() }
            )
        }

        // Manual Attendance Dialog (PIN Fallback)
        if (uiState.showManualDialog) {
            ManualAttendanceDialog(
                employees = activeEmployees,
                onConfirm = { emp, type ->
                    viewModel.manualRecordAttendance(emp, type)
                },
                onDismiss = { viewModel.closeManualDialog() }
            )
        }
    }
}

@Composable
fun MinimalKioskHeader(
    companyName: String,
    timeString: String,
    dateString: String,
    locationAddress: String,
    isGeofenceOk: Boolean,
    cloudStatus: CloudSyncStatus,
    onOpenAdmin: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = KioskDarkSurface,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, KioskCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Company & Location Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyanNeon.copy(alpha = 0.15f))
                        .border(1.dp, CyanNeon, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = if (isGeofenceOk) EmeraldSuccess else AmberCheckout,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = companyName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                    Text(
                        text = locationAddress,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 1
                    )
                }
            }

            // Digital Clock & Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Live Digital Clock
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = timeString,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanNeonLight,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = dateString,
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                // Cloud Online Dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (cloudStatus == CloudSyncStatus.SYNCED_ONLINE) EmeraldSuccess else CyanNeon)
                )

                // Admin Button
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenAdmin() },
                    color = Color(0xFF1E293B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin",
                            tint = CyanNeon,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Admin",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MinimalAttendanceTicker(
    recentAttendance: List<AttendanceRecord>,
    hasEnrolledEmployees: Boolean,
    hasRecognizedFace: Boolean
) {
    val lastRecord = recentAttendance.firstOrNull()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = KioskDarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, KioskCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (lastRecord != null) {
                val isCheckIn = lastRecord.type == AttendanceType.CHECK_IN
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isCheckIn) EmeraldSuccess else AmberCheckout,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Presensi Terakhir: ${lastRecord.employeeName} (${if (isCheckIn) "Masuk" else "Pulang"}) • ${lastRecord.formattedTime}",
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Cloud Synced",
                        fontSize = 11.sp,
                        color = EmeraldSuccess,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (hasRecognizedFace) EmeraldSuccess else CyanNeon)
                    )
                    Text(
                        text = if (hasRecognizedFace) "Wajah Terdeteksi • Acungkan 👍 Masuk atau 🖐️ Pulang" else if (hasEnrolledEmployees) "Kamera Siap • Posisikan Wajah di Depan Kamera" else "Belum ada karyawan terdaftar • Buka Admin untuk menambah",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Text(
                    text = if (hasRecognizedFace) "AI Vision Siap" else "Standby",
                    fontSize = 11.sp,
                    color = CyanNeon
                )
            }
        }
    }
}
