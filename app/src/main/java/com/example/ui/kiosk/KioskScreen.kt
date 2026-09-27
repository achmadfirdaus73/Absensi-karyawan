package com.example.ui.kiosk

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.biometrics.BiometricScanState
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceType
import com.example.data.model.Employee
import com.example.data.model.GestureType
import com.example.data.model.PunctualityStatus
import com.example.network.CloudSyncStatus
import com.example.ui.theme.AmberCheckout
import com.example.ui.theme.AmberCheckoutLight
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.CyanNeonLight
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.EmeraldSuccessLight
import com.example.ui.theme.KioskCardBackground
import com.example.ui.theme.KioskCardBorder
import com.example.ui.theme.KioskDarkBackground
import com.example.ui.theme.KioskDarkSurface
import com.example.ui.theme.RoseError
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

    var selectedSimEmployee by remember { mutableStateOf<Employee?>(null) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(KioskDarkBackground)
    ) {
        val isWideScreen = maxWidth > 720.dp

        if (isWideScreen) {
            // WIDE TABLET LANDSCAPE LAYOUT
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                KioskTopHeaderBar(
                    timeString = uiState.currentTimeString,
                    dateString = uiState.currentDateString,
                    companyName = kioskSettings?.companyName ?: "PT DIGITAL NUSANTARA UTAMA",
                    kioskName = kioskSettings?.kioskName ?: "Wall Tablet Kiosk",
                    locationAddress = uiState.locationInfo.address,
                    isGeofenceOk = uiState.locationInfo.isWithinOfficeGeofence,
                    cloudStatus = uiState.cloudStatus,
                    onOpenAdmin = onOpenAdmin,
                    onRefreshLocation = { viewModel.refreshLocation() }
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left Column: Live Camera + Biometric HUD + Interactive Action Bar
                    Column(
                        modifier = Modifier
                            .weight(1.25f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .border(1.5.dp, KioskCardBorder, RoundedCornerShape(24.dp))
                        ) {
                            CameraPreview(
                                isFrontCamera = uiState.isFrontCamera,
                                modifier = Modifier.fillMaxSize()
                            )

                            BiometricHudOverlay(
                                scanState = uiState.scanState,
                                recognizedEmployee = uiState.recognizedEmployee ?: selectedSimEmployee,
                                detectedGesture = uiState.detectedGesture
                            )

                            IconButton(
                                onClick = { viewModel.toggleCamera() },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(14.dp)
                                    .background(Color(0xAA0F172A), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlipCameraAndroid,
                                    contentDescription = "Ganti Kamera",
                                    tint = CyanNeon
                                )
                            }
                        }

                        EmployeeInteractiveActionBar(
                            employees = activeEmployees,
                            selectedEmployee = selectedSimEmployee ?: activeEmployees.firstOrNull(),
                            onSelectEmployee = { selectedSimEmployee = it },
                            onCheckInThumbsUp = { emp ->
                                viewModel.processBiometricAttendance(emp, GestureType.THUMBS_UP)
                            },
                            onCheckOut5Fingers = { emp ->
                                viewModel.processBiometricAttendance(emp, GestureType.OPEN_HAND)
                            },
                            onManualAttendance = { viewModel.openManualDialog() },
                            isProcessing = uiState.isProcessingAttendance
                        )
                    }

                    // Right Column: Summary Stats & Recent Attendance Feed
                    Column(
                        modifier = Modifier
                            .weight(0.95f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AttendanceSummaryStatsCards(recentAttendance = recentAttendance)

                        RecentAttendanceFeedCard(
                            recentAttendance = recentAttendance,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        } else {
            // VERTICAL / PHONE / PORTRAIT SCROLLABLE LAYOUT
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Responsive Header for phone screens
                KioskMobileHeaderBar(
                    timeString = uiState.currentTimeString,
                    dateString = uiState.currentDateString,
                    companyName = kioskSettings?.companyName ?: "PT DIGITAL NUSANTARA UTAMA",
                    kioskName = kioskSettings?.kioskName ?: "Wall Tablet Kiosk",
                    locationAddress = uiState.locationInfo.address,
                    isGeofenceOk = uiState.locationInfo.isWithinOfficeGeofence,
                    cloudStatus = uiState.cloudStatus,
                    onOpenAdmin = onOpenAdmin
                )

                // Camera Viewport & HUD Scanner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.5.dp, KioskCardBorder, RoundedCornerShape(20.dp))
                ) {
                    CameraPreview(
                        isFrontCamera = uiState.isFrontCamera,
                        modifier = Modifier.fillMaxSize()
                    )

                    BiometricHudOverlay(
                        scanState = uiState.scanState,
                        recognizedEmployee = uiState.recognizedEmployee ?: selectedSimEmployee,
                        detectedGesture = uiState.detectedGesture
                    )

                    IconButton(
                        onClick = { viewModel.toggleCamera() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .background(Color(0xAA0F172A), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlipCameraAndroid,
                            contentDescription = "Ganti Kamera",
                            tint = CyanNeon,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Interactive Employee Selector & Realtime Buttons (Masuk 👍 & Pulang 🖐️)
                EmployeeInteractiveActionBar(
                    employees = activeEmployees,
                    selectedEmployee = selectedSimEmployee ?: activeEmployees.firstOrNull(),
                    onSelectEmployee = { selectedSimEmployee = it },
                    onCheckInThumbsUp = { emp ->
                        viewModel.processBiometricAttendance(emp, GestureType.THUMBS_UP)
                    },
                    onCheckOut5Fingers = { emp ->
                        viewModel.processBiometricAttendance(emp, GestureType.OPEN_HAND)
                    },
                    onManualAttendance = { viewModel.openManualDialog() },
                    isProcessing = uiState.isProcessingAttendance
                )

                // Stats Cards
                AttendanceSummaryStatsCards(recentAttendance = recentAttendance)

                // Recent Attendance List (Fixed height scrollable container on phone)
                RecentAttendanceFeedCard(
                    recentAttendance = recentAttendance,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Dialogs
        if (uiState.showSuccessDialog && uiState.lastVerifiedRecord != null) {
            AttendanceSuccessDialog(
                record = uiState.lastVerifiedRecord!!,
                onDismiss = { viewModel.dismissSuccessDialog() }
            )
        }

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
fun KioskMobileHeaderBar(
    timeString: String,
    dateString: String,
    companyName: String,
    kioskName: String,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Brand & Admin Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyanNeon.copy(alpha = 0.2f))
                            .border(1.dp, CyanNeon, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Logo",
                            tint = CyanNeon,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = companyName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = kioskName,
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

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
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Admin",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Bottom Row: Clock + Location Tag + Cloud Sync Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = timeString,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanNeonLight,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = dateString,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (cloudStatus) {
                        CloudSyncStatus.SYNCED_ONLINE -> EmeraldSuccess.copy(alpha = 0.15f)
                        CloudSyncStatus.SYNCING -> CyanNeon.copy(alpha = 0.15f)
                        else -> AmberCheckout.copy(alpha = 0.15f)
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        when (cloudStatus) {
                            CloudSyncStatus.SYNCED_ONLINE -> EmeraldSuccess.copy(alpha = 0.4f)
                            CloudSyncStatus.SYNCING -> CyanNeon.copy(alpha = 0.4f)
                            else -> AmberCheckout.copy(alpha = 0.4f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Cloud",
                            tint = if (cloudStatus == CloudSyncStatus.SYNCED_ONLINE) EmeraldSuccess else CyanNeon,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (cloudStatus == CloudSyncStatus.SYNCED_ONLINE) "Cloud Online" else "Syncing...",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KioskTopHeaderBar(
    timeString: String,
    dateString: String,
    companyName: String,
    kioskName: String,
    locationAddress: String,
    isGeofenceOk: Boolean,
    cloudStatus: CloudSyncStatus,
    onOpenAdmin: () -> Unit,
    onRefreshLocation: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = KioskDarkSurface,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, KioskCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Company & Location Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyanNeon.copy(alpha = 0.2f))
                        .border(1.5.dp, CyanNeon, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Logo",
                        tint = CyanNeon,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column {
                    Text(
                        text = companyName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Lokasi",
                            tint = if (isGeofenceOk) EmeraldSuccess else AmberCheckout,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "$kioskName • $locationAddress",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            // Big Clock, Cloud Status & Admin Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cloud Sync Status Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = when (cloudStatus) {
                        CloudSyncStatus.SYNCED_ONLINE -> EmeraldSuccess.copy(alpha = 0.15f)
                        CloudSyncStatus.SYNCING -> CyanNeon.copy(alpha = 0.15f)
                        else -> AmberCheckout.copy(alpha = 0.15f)
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        when (cloudStatus) {
                            CloudSyncStatus.SYNCED_ONLINE -> EmeraldSuccess.copy(alpha = 0.4f)
                            CloudSyncStatus.SYNCING -> CyanNeon.copy(alpha = 0.4f)
                            else -> AmberCheckout.copy(alpha = 0.4f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Cloud Sync",
                            tint = if (cloudStatus == CloudSyncStatus.SYNCED_ONLINE) EmeraldSuccess else CyanNeon,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (cloudStatus == CloudSyncStatus.SYNCED_ONLINE) "Cloud Online" else "Sinkronisasi...",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                // Digital Clock
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = timeString,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanNeonLight,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = dateString,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                // Admin Button
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenAdmin() },
                    color = Color(0xFF1E293B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin",
                            tint = CyanNeon,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Admin",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmployeeInteractiveActionBar(
    employees: List<Employee>,
    selectedEmployee: Employee?,
    onSelectEmployee: (Employee) -> Unit,
    onCheckInThumbsUp: (Employee) -> Unit,
    onCheckOut5Fingers: (Employee) -> Unit,
    onManualAttendance: () -> Unit,
    isProcessing: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = KioskDarkSurface,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, KioskCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Employee Quick Detection / Selector Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "👤 Karyawan di Depan Kamera (Wajah):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF94A3B8)
                )

                Text(
                    text = "Klik nama untuk pilih",
                    fontSize = 11.sp,
                    color = CyanNeon
                )
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(employees) { emp ->
                    val isSelected = selectedEmployee?.id == emp.id
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectEmployee(emp) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) CyanNeon.copy(alpha = 0.25f) else KioskCardBackground,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, CyanNeon) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(emp.photoUri.ifBlank { "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=80" })
                                    .crossfade(true)
                                    .build(),
                                contentDescription = emp.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                            )
                            Column {
                                Text(
                                    text = emp.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = emp.employeeCode,
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
            }

            // Big Action Buttons for Real-time Gesture Presensi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Button Check-In (Thumbs Up 👍 Masuk)
                Button(
                    onClick = {
                        selectedEmployee?.let { onCheckInThumbsUp(it) }
                    },
                    enabled = selectedEmployee != null && !isProcessing,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldSuccess,
                        disabledContainerColor = Color(0xFF1E293B)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbUp,
                            contentDescription = "Jempol",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "MASUK (👍 SIP)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = "Wajah + Jempol",
                                fontSize = 9.sp,
                                color = Color(0xFF064E3B)
                            )
                        }
                    }
                }

                // Button Check-Out (5 Fingers 🖐️ Pulang)
                Button(
                    onClick = {
                        selectedEmployee?.let { onCheckOut5Fingers(it) }
                    },
                    enabled = selectedEmployee != null && !isProcessing,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberCheckout,
                        disabledContainerColor = Color(0xFF1E293B)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PanTool,
                            contentDescription = "5 Jari",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "PULANG (🖐️ 5 JARI)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = "Wajah + 5 Jari",
                                fontSize = 9.sp,
                                color = Color(0xFF78350F)
                            )
                        }
                    }
                }

                // Manual PIN Fallback button
                OutlinedButton(
                    onClick = onManualAttendance,
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = "Manual",
                        tint = CyanNeon,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AttendanceSummaryStatsCards(recentAttendance: List<AttendanceRecord>) {
    val checkIns = recentAttendance.count { it.type == AttendanceType.CHECK_IN }
    val checkOuts = recentAttendance.count { it.type == AttendanceType.CHECK_OUT }
    val lates = recentAttendance.count { it.punctualityStatus == PunctualityStatus.LATE }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Masuk Count
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            color = KioskDarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Hadir Masuk", fontSize = 11.sp, color = Color(0xFF94A3B8))
                Text(
                    text = "$checkIns",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldSuccessLight
                )
                Text(text = "👍 Verified", fontSize = 9.sp, color = EmeraldSuccess)
            }
        }

        // Pulang Count
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            color = KioskDarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, AmberCheckout.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Sudah Pulang", fontSize = 11.sp, color = Color(0xFF94A3B8))
                Text(
                    text = "$checkOuts",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberCheckoutLight
                )
                Text(text = "🖐️ Verified", fontSize = 9.sp, color = AmberCheckout)
            }
        }

        // Late Count
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            color = KioskDarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, RoseError.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Terlambat", fontSize = 11.sp, color = Color(0xFF94A3B8))
                Text(
                    text = "$lates",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoseError
                )
                Text(text = "> Jam Shift", fontSize = 9.sp, color = Color(0xFFFDA4AF))
            }
        }
    }
}

@Composable
fun RecentAttendanceFeedCard(
    recentAttendance: List<AttendanceRecord>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = KioskDarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, KioskCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(EmeraldSuccess)
                    )
                    Text(
                        text = "Live Log Presensi",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Text(
                    text = "${recentAttendance.size} Catatan",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (recentAttendance.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Belum ada presensi hari ini",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(recentAttendance) { record ->
                        val isCheckIn = record.type == AttendanceType.CHECK_IN
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = KioskCardBackground
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(record.photoSnapshotUri.ifBlank { "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=80" })
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = record.employeeName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = record.employeeName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${record.department} • ${record.formattedTime}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }

                                // Type & Gesture Tag
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCheckIn) EmeraldSuccess.copy(alpha = 0.2f) else AmberCheckout.copy(alpha = 0.2f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (record.gestureType == GestureType.THUMBS_UP) Icons.Default.ThumbUp else Icons.Default.PanTool,
                                            contentDescription = null,
                                            tint = if (isCheckIn) EmeraldSuccess else AmberCheckout,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = if (isCheckIn) "Masuk" else "Pulang",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCheckIn) EmeraldSuccessLight else AmberCheckoutLight
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
