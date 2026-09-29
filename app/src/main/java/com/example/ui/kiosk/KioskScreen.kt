package com.example.ui.kiosk

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.data.model.*
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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val employees by repository.allActiveEmployees.collectAsState(initial = emptyList())
    val kioskSettingsState by repository.kioskSettingsFlow.collectAsState(initial = null)
    val settings = kioskSettingsState ?: KioskSettings()

    // Camera Permission State
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var useFrontCamera by remember { mutableStateOf(true) }
    var currentTimeStr by remember { mutableStateOf("") }
    var currentDateStr by remember { mutableStateOf("") }

    var lastSuccessRecord by remember { mutableStateOf<AttendanceRecord?>(null) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showAdminPinDialog by remember { mutableStateOf(false) }
    var adminPinInput by remember { mutableStateOf("") }
    var adminPinError by remember { mutableStateOf(false) }

    // Currently identified / active person in front of the camera
    var selectedEmployee by remember { mutableStateOf<Employee?>(null) }
    var isProcessingAttendance by remember { mutableStateOf(false) }

    // Auto-select first employee when list loads if none selected
    LaunchedEffect(employees) {
        if (employees.isNotEmpty() && selectedEmployee == null) {
            selectedEmployee = employees.first()
        }
    }

    // Scanning animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scanPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanPulse"
    )

    // Clock ticker
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
        // 1. Fullscreen Live Camera Background
        if (hasCameraPermission) {
            LiveCameraPreview(
                modifier = Modifier.fillMaxSize(),
                useFrontCamera = useFrontCamera
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBg),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.VideocamOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Izin Kamera Diperlukan", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Text("Izinkan Kamera", color = DarkBg, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Dark gradient overlays for pristine readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DarkBg.copy(alpha = 0.75f),
                            Color.Transparent,
                            DarkBg.copy(alpha = 0.90f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // 2. Center Biometric Face Target HUD
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 120.dp),
            contentAlignment = Alignment.Center
        ) {
            // Scanner Ring
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .scale(scanPulseScale)
                    .border(
                        width = 3.dp,
                        brush = Brush.sweepGradient(listOf(NeonCyan, NeonGreen, NeonPurple, NeonCyan)),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Inner Target Frame
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .background(Color.Black.copy(alpha = 0.25f), CircleShape)
                        .border(1.5.dp, NeonCyan.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = null,
                            tint = if (selectedEmployee != null) NeonCyan else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (selectedEmployee != null) "Wajah Terdeteksi" else "Posisikan Wajah",
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // 3. Top Header: Company Name, Live Clock & Admin Lock
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Company & Status Badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CardBg.copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(NeonGreen, CircleShape)
                        )
                        Column {
                            Text(
                                text = settings.companyName,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "LIVE BIOMETRIC KIOSK",
                                color = NeonCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                // Top Actions: Camera Switch, PIN, Admin
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Switch Camera
                    IconButton(
                        onClick = { useFrontCamera = !useFrontCamera },
                        modifier = Modifier
                            .size(40.dp)
                            .background(CardBg.copy(alpha = 0.85f), CircleShape)
                            .border(1.dp, CardBorder, CircleShape)
                    ) {
                        Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Ganti Kamera", tint = NeonCyan, modifier = Modifier.size(20.dp))
                    }

                    // Manual PIN Attendance
                    IconButton(
                        onClick = { showPinDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .background(CardBg.copy(alpha = 0.85f), CircleShape)
                            .border(1.dp, CardBorder, CircleShape)
                    ) {
                        Icon(Icons.Default.Pin, contentDescription = "Absen PIN", tint = NeonYellow, modifier = Modifier.size(20.dp))
                    }

                    // Admin Dashboard Lock
                    IconButton(
                        onClick = {
                            adminPinInput = ""
                            adminPinError = false
                            showAdminPinDialog = true
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(CardBg.copy(alpha = 0.85f), CircleShape)
                            .border(1.dp, CardBorder, CircleShape)
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin", tint = TextPrimary, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Time & Date Pill
            Surface(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                shape = RoundedCornerShape(20.dp),
                color = CardBg.copy(alpha = 0.75f),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Text(
                        text = "${currentTimeStr.ifEmpty { "08:00:00" }} • ${currentDateStr.ifEmpty { "Hari ini" }}",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 4. Bottom Interactive Overlay: EMPLOYEE BADGE + ABSEN MASUK & PULANG BUTTONS
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            if (employees.isEmpty()) {
                // Empty state if no employee has been registered yet
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, NeonYellow.copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg.copy(alpha = 0.95f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = NeonYellow, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Belum Ada Karyawan Terdaftar",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Silakan buka Admin Dashboard untuk menambahkan karyawan dan konfigurasi role & shift.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                adminPinInput = ""
                                adminPinError = false
                                showAdminPinDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = DarkBg)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Buka Admin Dashboard", color = DarkBg, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                val emp = selectedEmployee ?: employees.first()
                val shiftResolution = repository.resolveShiftForEmployee(emp, settings)

                // Quick Switcher Row (If multiple employees exist, lets them tap easily or switch)
                if (employees.size > 1) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(employees, key = { it.id }) { item ->
                            val isSelected = item.id == emp.id
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) NeonCyan.copy(alpha = 0.25f) else CardBg.copy(alpha = 0.85f),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) NeonCyan else CardBorder
                                ),
                                modifier = Modifier.clickable {
                                    selectedEmployee = item
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(if (isSelected) NeonCyan else TextMuted, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = item.name.take(1).uppercase(),
                                            color = DarkBg,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = item.name,
                                        color = if (isSelected) NeonCyan else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                // PRIMARY POPUP CARD: Nama, Jabatan/Role, Jam Kerja, Tombol Masuk & Pulang
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, NeonCyan.copy(alpha = 0.8f), RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg.copy(alpha = 0.95f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        // Top Identity Row: Avatar + Name + Role Badge + Shift Schedule
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Avatar circle
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .background(NeonCyan.copy(alpha = 0.2f), CircleShape)
                                        .border(2.dp, NeonCyan, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = emp.name.take(2).uppercase(),
                                        color = NeonCyan,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp
                                    )
                                }

                                Column {
                                    // 👤 Employee Name
                                    Text(
                                        text = emp.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextPrimary
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // 💼 Badges: Role & Department & NIK
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            color = NeonPurple.copy(alpha = 0.25f),
                                            shape = RoundedCornerShape(6.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple.copy(alpha = 0.5f))
                                        ) {
                                            Text(
                                                text = emp.role.uppercase(),
                                                color = NeonPurple,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Text(text = "•", color = TextMuted)
                                        Text(
                                            text = emp.division,
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(text = "•", color = TextMuted)
                                        Text(
                                            text = "NIK: ${emp.nik}",
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // ⏰ Work Shift / Schedule Info Badge
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = InputBg.copy(alpha = 0.8f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "Jadwal Kerja:",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                                Text(
                                    text = "${shiftResolution.shiftName} (${shiftResolution.startTime} - ${shiftResolution.endTime})",
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 🚀 TOMBOL ABSEN MASUK & ABSEN PULANG
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // TOMBOL ABSEN MASUK
                            Button(
                                onClick = {
                                    if (!isProcessingAttendance) {
                                        isProcessingAttendance = true
                                        coroutineScope.launch {
                                            val record = repository.processAttendance(
                                                employee = emp,
                                                checkType = CheckType.CHECK_IN,
                                                confidenceScore = 0.98f
                                            )
                                            val statusText = if (record.status == AttendanceStatus.ON_TIME) "Tepat Waktu" else "Terlambat"
                                            audioHelper.speak("Terima kasih ${emp.name}, berhasil Absen Masuk $statusText!")
                                            lastSuccessRecord = record
                                            isProcessingAttendance = false
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Login, contentDescription = null, tint = DarkBg, modifier = Modifier.size(24.dp))
                                    Text(
                                        text = "ABSEN MASUK",
                                        color = DarkBg,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp
                                    )
                                }
                            }

                            // TOMBOL ABSEN PULANG
                            Button(
                                onClick = {
                                    if (!isProcessingAttendance) {
                                        isProcessingAttendance = true
                                        coroutineScope.launch {
                                            val record = repository.processAttendance(
                                                employee = emp,
                                                checkType = CheckType.CHECK_OUT,
                                                confidenceScore = 0.98f
                                            )
                                            val statusText = if (record.status == AttendanceStatus.OVERTIME) "Lembur" else "Berhasil"
                                            audioHelper.speak("Terima kasih ${emp.name}, berhasil Absen Pulang $statusText!")
                                            lastSuccessRecord = record
                                            isProcessingAttendance = false
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Logout, contentDescription = null, tint = DarkBg, modifier = Modifier.size(24.dp))
                                    Text(
                                        text = "ABSEN PULANG",
                                        color = DarkBg,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
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

    // PIN Attendance Fallback Dialog
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
                        val actionName = if (checkType == CheckType.CHECK_IN) "Masuk" else "Pulang"
                        audioHelper.speak("Terima kasih ${employee.name}, berhasil Absen $actionName!")
                        lastSuccessRecord = record
                        showPinDialog = false
                    } else {
                        audioHelper.speak("NIK atau PIN tidak cocok!")
                    }
                }
            }
        )
    }

    // Admin Verification PIN Modal
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
                    Text(
                        text = "Verifikasi Akses Admin",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Masukkan PIN Kiosk Admin untuk membuka Dashboard Pendaftaran & Pengaturan",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = adminPinInput,
                        onValueChange = {
                            if (it.length <= 6) {
                                adminPinInput = it
                                adminPinError = false
                            }
                        },
                        label = { Text("PIN Admin (Default: 8888)") },
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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
