package com.example.ui.kiosk

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.data.repository.AttendanceRepository
import com.example.ui.theme.*
import com.example.util.AudioFeedbackHelper
import com.google.mlkit.vision.face.Face
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

    // Live Face Recognition State from ML Kit
    var isLiveFaceDetected by remember { mutableStateOf(false) }
    var liveFaceCount by remember { mutableStateOf(0) }
    var lastFaceDetectionTime by remember { mutableStateOf(0L) }

    // Currently identified / active person in front of the camera
    var selectedEmployee by remember { mutableStateOf<Employee?>(null) }
    var isProcessingAttendance by remember { mutableStateOf(false) }

    // Auto-select first employee when list loads if none selected
    LaunchedEffect(employees) {
        if (employees.isNotEmpty() && selectedEmployee == null) {
            selectedEmployee = employees.first()
        } else if (employees.isEmpty()) {
            selectedEmployee = null
        }
    }

    // Reset face detected flag if no face events in 1.5 seconds
    LaunchedEffect(lastFaceDetectionTime) {
        if (lastFaceDetectionTime > 0) {
            delay(1500)
            if (System.currentTimeMillis() - lastFaceDetectionTime >= 1500) {
                isLiveFaceDetected = false
            }
        }
    }

    // Biometric Scanner Pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scanPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanPulse"
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
            .background(LightBg)
    ) {
        // 1. FULLSCREEN LIVE CAMERA
        if (hasCameraPermission) {
            LiveCameraPreview(
                modifier = Modifier.fillMaxSize(),
                useFrontCamera = useFrontCamera,
                onFacesDetected = { faces: List<Face> ->
                    if (faces.isNotEmpty()) {
                        isLiveFaceDetected = true
                        liveFaceCount = faces.size
                        lastFaceDetectionTime = System.currentTimeMillis()
                        if (selectedEmployee == null && employees.isNotEmpty()) {
                            selectedEmployee = employees.first()
                        }
                    }
                }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightBg),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.VideocamOff, contentDescription = null, tint = TextSecondaryLight, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Izin Kamera Diperlukan", color = TextPrimaryLight, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Izinkan Kamera", color = PureWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Clean subtle light gradient for top and bottom readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            PureWhite.copy(alpha = 0.85f),
                            Color.Transparent,
                            PureWhite.copy(alpha = 0.80f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // 2. 🔲 MINIMALIST BIOMETRIC FACE RETICLE
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 80.dp)
                .size(230.dp)
                .scale(scanPulseScale),
            contentAlignment = Alignment.Center
        ) {
            val bracketColor = if (isLiveFaceDetected) AccentGreen else PrimaryNavy

            Canvas(modifier = Modifier.fillMaxSize()) {
                val cornerLength = 32.dp.toPx()
                val strokeWidth = 3.dp.toPx()

                // Top-Left Corner
                drawLine(color = bracketColor, start = Offset(0f, cornerLength), end = Offset(0f, 0f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawLine(color = bracketColor, start = Offset(0f, 0f), end = Offset(cornerLength, 0f), strokeWidth = strokeWidth, cap = StrokeCap.Round)

                // Top-Right Corner
                drawLine(color = bracketColor, start = Offset(size.width - cornerLength, 0f), end = Offset(size.width, 0f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawLine(color = bracketColor, start = Offset(size.width, 0f), end = Offset(size.width, cornerLength), strokeWidth = strokeWidth, cap = StrokeCap.Round)

                // Bottom-Left Corner
                drawLine(color = bracketColor, start = Offset(0f, size.height - cornerLength), end = Offset(0f, size.height), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawLine(color = bracketColor, start = Offset(0f, size.height), end = Offset(cornerLength, size.height), strokeWidth = strokeWidth, cap = StrokeCap.Round)

                // Bottom-Right Corner
                drawLine(color = bracketColor, start = Offset(size.width - cornerLength, size.height), end = Offset(size.width, size.height), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawLine(color = bracketColor, start = Offset(size.width, size.height - cornerLength), end = Offset(size.width, size.height), strokeWidth = strokeWidth, cap = StrokeCap.Round)
            }

            // Center Guide
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (selectedEmployee?.photoUri != null) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .border(2.dp, bracketColor, CircleShape)
                    ) {
                        AsyncImage(
                            model = selectedEmployee!!.photoUri,
                            contentDescription = "Face",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = null,
                        tint = bracketColor.copy(alpha = 0.8f),
                        modifier = Modifier.size(52.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = PureWhite.copy(alpha = 0.95f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderLight),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (isLiveFaceDetected) {
                            Box(modifier = Modifier.size(6.dp).background(AccentGreen, CircleShape))
                        }
                        Text(
                            text = if (isLiveFaceDetected) "Wajah Terdeteksi" else if (selectedEmployee != null) "Wajah Siap" else "Arahkan Wajah",
                            color = bracketColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // 3. 🏢 CLEAN EXECUTIVE WHITE TOP HEADER
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Company Name & Address
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PureWhite.copy(alpha = 0.96f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderLight),
                    shadowElevation = 2.dp,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (isLiveFaceDetected) AccentGreen else PrimaryBlue, CircleShape)
                        )
                        Column {
                            Text(
                                text = settings.companyName,
                                color = TextPrimaryLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${settings.address} • ${currentTimeStr.ifEmpty { "08:00:00" }}",
                                color = TextSecondaryLight,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Right: Clean Functional Actions
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Switch Camera
                    IconButton(
                        onClick = { useFrontCamera = !useFrontCamera },
                        modifier = Modifier
                            .size(38.dp)
                            .background(PureWhite, CircleShape)
                            .border(1.dp, CardBorderLight, CircleShape)
                            .shadow(1.dp, CircleShape)
                    ) {
                        Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Ganti Kamera", tint = PrimaryNavy, modifier = Modifier.size(18.dp))
                    }

                    // Absen Manual PIN
                    IconButton(
                        onClick = { showPinDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .background(PureWhite, CircleShape)
                            .border(1.dp, CardBorderLight, CircleShape)
                            .shadow(1.dp, CircleShape)
                    ) {
                        Icon(Icons.Default.Pin, contentDescription = "Absen PIN", tint = PrimaryNavy, modifier = Modifier.size(18.dp))
                    }

                    // Admin Access
                    Button(
                        onClick = {
                            adminPinInput = ""
                            adminPinError = false
                            showAdminPinDialog = true
                        },
                        modifier = Modifier.height(38.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text("Admin", color = PureWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Top Alert if no employees yet
            if (employees.isEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = InputBgLight,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Belum ada data karyawan. Buka menu Admin di atas untuk mendaftarkan.",
                            color = TextSecondaryLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 4. 🪪 REFINED WHITE COMPACT BADGE CARD
        if (employees.isNotEmpty()) {
            val emp = selectedEmployee ?: employees.first()
            val shiftResolution = repository.resolveShiftForEmployee(emp, settings)

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                // Quick Employee Switcher (Compact Minimalist)
                if (employees.size > 1) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(employees, key = { it.id }) { item ->
                            val isSelected = item.id == emp.id
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) PrimaryNavy else PureWhite,
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) PrimaryNavy else CardBorderLight
                                ),
                                shadowElevation = if (isSelected) 2.dp else 0.dp,
                                modifier = Modifier.clickable {
                                    selectedEmployee = item
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (item.photoUri != null) {
                                        AsyncImage(
                                            model = item.photoUri,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Text(
                                        text = item.name,
                                        color = if (isSelected) PureWhite else TextPrimaryLight,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                // PURE WHITE CARD
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderLight)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        // Top Identity Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Photo / Initials
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(InputBgLight)
                                        .border(1.dp, CardBorderLight, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!emp.photoUri.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = emp.photoUri,
                                            contentDescription = emp.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text(
                                            text = emp.name.take(2).uppercase(),
                                            color = PrimaryNavy,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }
                                }

                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = emp.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = TextPrimaryLight
                                        )

                                        Surface(
                                            color = InputBgLight,
                                            shape = RoundedCornerShape(4.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderLight)
                                        ) {
                                            Text(
                                                text = emp.role.uppercase(),
                                                color = PrimaryNavy,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "NIK: ${emp.nik} • ${emp.division}",
                                            color = TextSecondaryLight,
                                            fontSize = 11.sp
                                        )
                                        Text(text = "•", color = TextMutedLight)
                                        Text(
                                            text = "${shiftResolution.shiftName} (${shiftResolution.startTime}-${shiftResolution.endTime})",
                                            color = PrimaryBlue,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Compact Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // ABSEN MASUK
                            Button(
                                onClick = {
                                    if (!isProcessingAttendance) {
                                        isProcessingAttendance = true
                                        coroutineScope.launch {
                                            val record = repository.processAttendance(
                                                employee = emp,
                                                checkType = CheckType.CHECK_IN,
                                                confidenceScore = if (isLiveFaceDetected) 0.99f else 0.95f
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
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
                            ) {
                                Text("ABSEN MASUK", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            // ABSEN PULANG
                            Button(
                                onClick = {
                                    if (!isProcessingAttendance) {
                                        isProcessingAttendance = true
                                        coroutineScope.launch {
                                            val record = repository.processAttendance(
                                                employee = emp,
                                                checkType = CheckType.CHECK_OUT,
                                                confidenceScore = if (isLiveFaceDetected) 0.99f else 0.95f
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
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                            ) {
                                Text("ABSEN PULANG", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                    .shadow(8.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderLight)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Verifikasi Admin",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimaryLight,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Masukkan PIN Kiosk Admin untuk membuka Dashboard",
                        color = TextSecondaryLight,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

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
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryNavy,
                            unfocusedBorderColor = CardBorderLight,
                            focusedTextColor = TextPrimaryLight,
                            unfocusedTextColor = TextPrimaryLight
                        )
                    )

                    if (adminPinError) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "PIN Admin salah!", color = AccentRed, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAdminPinDialog = false },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderLight)
                        ) {
                            Text("Batal", color = TextSecondaryLight)
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
                            modifier = Modifier.weight(1f).height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Masuk", color = PureWhite, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
