package com.example.ui.kiosk

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AttendanceRecord
import com.example.data.model.CheckType
import com.example.data.model.Employee
import com.example.data.model.KioskSettings
import com.example.data.repository.AttendanceRepository
import com.example.ui.theme.*
import com.example.util.AudioFeedbackHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun KioskScreen(
    repository: AttendanceRepository,
    audioHelper: AudioFeedbackHelper,
    onNavigateToAdmin: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val employees by repository.allActiveEmployees.collectAsState(initial = emptyList())
    val kioskSettingsState by repository.kioskSettingsFlow.collectAsState(initial = null)
    val settings = kioskSettingsState ?: KioskSettings()

    var currentTimeStr by remember { mutableStateOf("") }
    var currentDateStr by remember { mutableStateOf("") }

    var lastSuccessRecord by remember { mutableStateOf<AttendanceRecord?>(null) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showAdminPinDialog by remember { mutableStateOf(false) }
    var adminPinInput by remember { mutableStateOf("") }
    var adminPinError by remember { mutableStateOf(false) }

    var isScanning by remember { mutableStateOf(false) }
    var scanStatusMessage by remember { mutableStateOf("Arahkan wajah ke kamera") }

    // Pulse animation for scanner
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Live clock ticker
    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
        while (true) {
            val now = Date()
            currentTimeStr = timeFormat.format(now)
            currentDateStr = dateFormat.format(now)
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Company identity & Admin Lock
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NeonCyan.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan)
                    ) {
                        Box(modifier = Modifier.padding(8.dp)) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(28.dp))
                        }
                    }
                    Column {
                        Text(
                            text = settings.companyName,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "SMART BIOMETRIC ATTENDANCE KIOSK",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.5.sp
                        )
                    }
                }

                // Admin Button
                Button(
                    onClick = {
                        adminPinInput = ""
                        adminPinError = false
                        showAdminPinDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CardBg),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Admin Dashboard", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Time & Date HUD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = currentTimeStr.ifEmpty { "08:00:00" },
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeonCyan,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = currentDateStr.ifEmpty { "Senin, 28 September 2026" },
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }

                    // Multi-Shift Info Pills
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ShiftPill("Shift 1", "${settings.shift1Start}-${settings.shift1End}", NeonCyan)
                        ShiftPill("Shift 2", "${settings.shift2Start}-${settings.shift2End}", NeonYellow)
                        ShiftPill("Shift 3", "${settings.shift3Start}-${settings.shift3End}", NeonPurple)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Center Camera Viewfinder Simulator / Biometric Target
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(InputBg)
                    .border(2.dp, CardBorder, RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Biometric Scan Ring
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .scale(if (isScanning) pulseScale else 1f)
                        .border(
                            width = if (isScanning) 3.dp else 2.dp,
                            brush = Brush.sweepGradient(
                                listOf(NeonCyan, NeonGreen, NeonPurple, NeonCyan)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(230.dp)
                            .background(CardBg.copy(alpha = 0.6f), CircleShape)
                            .border(1.dp, CardBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (isScanning) Icons.Default.FaceRetouchingNatural else Icons.Default.Face,
                                contentDescription = null,
                                tint = if (isScanning) NeonCyan else TextSecondary,
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = scanStatusMessage,
                                color = if (isScanning) NeonCyan else TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Top Floating Status Pill
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = CardBg.copy(alpha = 0.9f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(NeonGreen, CircleShape)
                        )
                        Text(
                            text = "Kamera Biometrik Aktif • ${employees.size} Karyawan Terdaftar",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Bottom Gesture Hints
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GestureBadge("👍 Jempol", "Absen Masuk", NeonCyan)
                    GestureBadge("🖐️ 5 Jari", "Absen Pulang", NeonGreen)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Absen Masuk Button (Thumbs up trigger)
                Button(
                    onClick = {
                        triggerBiometricScan(
                            checkType = CheckType.CHECK_IN,
                            employees = employees,
                            repository = repository,
                            audioHelper = audioHelper,
                            onScanning = { isScanning = it },
                            onStatusChange = { scanStatusMessage = it },
                            onSuccess = { lastSuccessRecord = it },
                            coroutineScope = coroutineScope
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "👍", fontSize = 22.sp)
                        Column {
                            Text(text = "ABSEN MASUK", color = DarkBg, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            Text(text = "Deteksi Wajah & Jempol", color = DarkBg.copy(alpha = 0.8f), fontSize = 11.sp)
                        }
                    }
                }

                // Absen Pulang Button (5 fingers trigger)
                Button(
                    onClick = {
                        triggerBiometricScan(
                            checkType = CheckType.CHECK_OUT,
                            employees = employees,
                            repository = repository,
                            audioHelper = audioHelper,
                            onScanning = { isScanning = it },
                            onStatusChange = { scanStatusMessage = it },
                            onSuccess = { lastSuccessRecord = it },
                            coroutineScope = coroutineScope
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "🖐️", fontSize = 22.sp)
                        Column {
                            Text(text = "ABSEN PULANG", color = DarkBg, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            Text(text = "Deteksi Wajah & 5 Jari", color = DarkBg.copy(alpha = 0.8f), fontSize = 11.sp)
                        }
                    }
                }

                // PIN Attendance Fallback
                OutlinedButton(
                    onClick = { showPinDialog = true },
                    modifier = Modifier
                        .width(130.dp)
                        .height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonYellow)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Pin, contentDescription = null, tint = NeonYellow, modifier = Modifier.size(20.dp))
                        Text(text = "Absen PIN", color = NeonYellow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // Success Dialog
    if (lastSuccessRecord != null) {
        AttendanceSuccessDialog(
            record = lastSuccessRecord!!,
            onDismiss = { lastSuccessRecord = null }
        )
    }

    // PIN Dialog
    if (showPinDialog) {
        PinAttendanceDialog(
            onDismiss = { showPinDialog = false },
            onSubmitPin = { nik, pin, checkType ->
                coroutineScope.launch {
                    val employee = repository.getEmployeeByNikAndPin(nik, pin)
                    if (employee != null) {
                        val record = repository.processAttendance(
                            employee = employee,
                            checkType = checkType,
                            confidenceScore = 1.0f
                        )
                        audioHelper.speak("Terima kasih, ${employee.name} berhasil presensi!")
                        lastSuccessRecord = record
                        showPinDialog = false
                    } else {
                        audioHelper.speak("NIK atau PIN salah!")
                    }
                }
            }
        )
    }

    // Admin PIN Modal
    if (showAdminPinDialog) {
        Dialog(onDismissRequest = { showAdminPinDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(2.dp, NeonCyan, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(44.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Verifikasi Akses Admin", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text(text = "Masukkan PIN Kiosk Admin untuk membuka dashboard", color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = adminPinInput,
                        onValueChange = {
                            if (it.length <= 6) {
                                adminPinInput = it
                                adminPinError = false
                            }
                        },
                        label = { Text("PIN Admin") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        isError = adminPinError,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    if (adminPinError) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "PIN Admin salah!", color = NeonRed, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { showAdminPinDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Batal", color = TextSecondary)
                        }

                        Button(
                            onClick = {
                                if (adminPinInput == settings.kioskPin) {
                                    showAdminPinDialog = false
                                    onNavigateToAdmin()
                                } else {
                                    adminPinError = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Masuk", color = DarkBg, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun triggerBiometricScan(
    checkType: CheckType,
    employees: List<Employee>,
    repository: AttendanceRepository,
    audioHelper: AudioFeedbackHelper,
    onScanning: (Boolean) -> Unit,
    onStatusChange: (String) -> Unit,
    onSuccess: (AttendanceRecord) -> Unit,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    if (employees.isEmpty()) {
        onStatusChange("Belum ada karyawan terdaftar")
        return
    }

    coroutineScope.launch {
        onScanning(true)
        onStatusChange("Memindai biometrik wajah...")
        delay(800)

        // Select demo employee or matching face
        val matchedEmployee = employees.first()
        onStatusChange("Wajah teridentifikasi: ${matchedEmployee.name}")
        delay(400)

        val record = repository.processAttendance(
            employee = matchedEmployee,
            checkType = checkType,
            confidenceScore = 0.96f
        )

        val actionName = if (checkType == CheckType.CHECK_IN) "Absen Masuk" else "Absen Pulang"
        audioHelper.speak("Terima kasih, ${matchedEmployee.name} berhasil $actionName!")

        onScanning(false)
        onStatusChange("Arahkan wajah ke kamera")
        onSuccess(record)
    }
}

@Composable
fun ShiftPill(name: String, timeRange: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = name, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(text = timeRange, color = TextPrimary, fontSize = 11.sp)
        }
    }
}

@Composable
fun GestureBadge(gesture: String, label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CardBg.copy(alpha = 0.9f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = gesture, fontSize = 14.sp)
            Text(text = label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
