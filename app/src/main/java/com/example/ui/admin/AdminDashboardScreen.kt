package com.example.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.AttendanceRepository
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    repository: AttendanceRepository,
    onNavigateBack: () -> Unit
) {
    BackHandler {
        onNavigateBack()
    }

    val coroutineScope = rememberCoroutineScope()
    val employees by repository.allActiveEmployees.collectAsState(initial = emptyList())
    val attendanceRecords by repository.allAttendanceRecords.collectAsState(initial = emptyList())
    val kioskSettingsState by repository.kioskSettingsFlow.collectAsState(initial = null)
    val notifications by repository.allNotifications.collectAsState(initial = emptyList())

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Karyawan, 1: Log Absensi, 2: Pengaturan, 3: Notifikasi
    var showRegisterDialog by remember { mutableStateOf(false) }
    var employeeToEdit by remember { mutableStateOf<Employee?>(null) }
    var employeeToDelete by remember { mutableStateOf<Employee?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    val currentSettings = kioskSettingsState ?: KioskSettings()

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = NeonCyan.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = NeonCyan)
                            }
                        }
                        Column {
                            Text(
                                text = "Admin Dashboard",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentSettings.companyName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali ke Kiosk", tint = NeonCyan)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Fullscreen, contentDescription = "Mode Kiosk", tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardBg)
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = {
                        employeeToEdit = null
                        showRegisterDialog = true
                    },
                    containerColor = NeonCyan,
                    contentColor = DarkBg,
                    shape = RoundedCornerShape(16.dp),
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                    text = { Text("Tambah Karyawan", fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Navigation Tabs
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = CardBg,
                contentColor = NeonCyan,
                divider = { HorizontalDivider(color = CardBorder) }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Data Karyawan (${employees.size})", fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Log Presensi (${attendanceRecords.size})", fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Pengaturan Kiosk", fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Notifikasi (${notifications.size})", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            // Tab Contents
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> EmployeesTab(
                        employees = employees,
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        onEditEmployee = { emp ->
                            employeeToEdit = emp
                            showRegisterDialog = true
                        },
                        onDeleteEmployee = { emp ->
                            employeeToDelete = emp
                        }
                    )
                    1 -> AttendanceLogsTab(
                        records = attendanceRecords,
                        onDeleteRecord = { id ->
                            coroutineScope.launch {
                                repository.deleteAttendance(id)
                                snackbarMessage = "Log presensi dihapus"
                            }
                        }
                    )
                    2 -> KioskSettingsTab(
                        settings = currentSettings,
                        onSaveSettings = { updated ->
                            coroutineScope.launch {
                                repository.saveKioskSettings(updated)
                                snackbarMessage = "Pengaturan berhasil disimpan!"
                            }
                        }
                    )
                    3 -> NotificationsTab(notifications = notifications)
                }
            }
        }
    }

    // Register / Edit Employee Dialog
    if (showRegisterDialog) {
        RegisterEmployeeDialog(
            employeeToEdit = employeeToEdit,
            kioskSettings = currentSettings,
            onDismiss = { showRegisterDialog = false },
            onSaveEmployee = { emp ->
                coroutineScope.launch {
                    if (employeeToEdit == null) {
                        repository.insertEmployee(emp)
                        snackbarMessage = "Karyawan ${emp.name} berhasil didaftarkan!"
                    } else {
                        repository.updateEmployee(emp)
                        snackbarMessage = "Data karyawan ${emp.name} berhasil diperbarui!"
                    }
                    showRegisterDialog = false
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (employeeToDelete != null) {
        AlertDialog(
            onDismissRequest = { employeeToDelete = null },
            containerColor = CardBg,
            title = { Text("Hapus Karyawan?", color = NeonRed, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Apakah Anda yakin ingin menghapus ${employeeToDelete?.name} (${employeeToDelete?.nik})? Data ini tidak bisa dikembalikan.",
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            employeeToDelete?.let { repository.deleteEmployee(it) }
                            employeeToDelete = null
                            snackbarMessage = "Karyawan telah dihapus"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed)
                ) {
                    Text("Hapus", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { employeeToDelete = null }) {
                    Text("Batal", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun EmployeesTab(
    employees: List<Employee>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onEditEmployee: (Employee) -> Unit,
    onDeleteEmployee: (Employee) -> Unit
) {
    val filteredEmployees = remember(employees, searchQuery) {
        if (searchQuery.isBlank()) employees
        else employees.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.nik.contains(searchQuery, ignoreCase = true) ||
                    it.role.contains(searchQuery, ignoreCase = true) ||
                    it.division.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Cari nama, NIK, role (Manager/Staff), divisi...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = CardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (filteredEmployees.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PeopleOutline, contentDescription = null, tint = TextMuted, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Belum ada data karyawan ditemukan", color = TextSecondary)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredEmployees, key = { it.id }) { emp ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(NeonCyan.copy(alpha = 0.15f), CircleShape)
                                        .border(1.dp, NeonCyan, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = emp.name.take(2).uppercase(),
                                        color = NeonCyan,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }

                                Column {
                                    Text(
                                        text = emp.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Role Badge
                                        Surface(
                                            color = NeonPurple.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = emp.role,
                                                color = NeonPurple,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Text(text = "•", color = TextMuted)
                                        Text(text = emp.division, color = TextSecondary, fontSize = 12.sp)
                                        Text(text = "•", color = TextMuted)
                                        Text(text = "NIK: ${emp.nik}", color = TextMuted, fontSize = 12.sp)
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Work Shift Badge
                                    val (shiftBadgeText, shiftBadgeColor) = when (emp.shiftType) {
                                        ShiftAssignmentType.SHIFT_1.name -> Pair("☀️ Shift 1 Tetap (Pagi)", NeonCyan)
                                        ShiftAssignmentType.SHIFT_2.name -> Pair("⛅ Shift 2 Tetap (Siang)", NeonYellow)
                                        ShiftAssignmentType.SHIFT_3.name -> Pair("🌙 Shift 3 Tetap (Malam)", NeonPurple)
                                        ShiftAssignmentType.MANUAL_CUSTOM.name -> Pair("✍️ Khusus: ${emp.customStartTime} - ${emp.customEndTime}", NeonGreen)
                                        else -> Pair("🔄 Shift Kantor (Auto-Detect)", NeonCyan)
                                    }

                                    Surface(
                                        color = shiftBadgeColor.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = shiftBadgeText,
                                            color = shiftBadgeColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = { onEditEmployee(emp) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = NeonCyan)
                                }
                                IconButton(onClick = { onDeleteEmployee(emp) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = NeonRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceLogsTab(
    records: List<AttendanceRecord>,
    onDeleteRecord: (Long) -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    if (records.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.EventBusy, contentDescription = null, tint = TextMuted, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Belum ada riwayat presensi tersimpan", color = TextSecondary)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(records, key = { it.id }) { record ->
                val (statusText, statusBg, statusColor) = when (record.status) {
                    AttendanceStatus.ON_TIME -> Triple("Tepat Waktu", NeonGreen.copy(alpha = 0.15f), NeonGreen)
                    AttendanceStatus.LATE -> Triple("Terlambat", NeonRed.copy(alpha = 0.15f), NeonRed)
                    AttendanceStatus.EARLY_DEPARTURE -> Triple("Pulang Awal", NeonYellow.copy(alpha = 0.15f), NeonYellow)
                    AttendanceStatus.OVERTIME -> Triple("Lembur", NeonPurple.copy(alpha = 0.15f), NeonPurple)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = record.employeeName,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 16.sp
                                )
                                Surface(
                                    color = NeonPurple.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = record.role,
                                        color = NeonPurple,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "NIK: ${record.nik}  •  ${record.shiftName}",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = if (record.checkType == CheckType.CHECK_IN) NeonCyan.copy(alpha = 0.15f) else NeonGreen.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (record.checkType == CheckType.CHECK_IN) "ABSEN MASUK" else "ABSEN PULANG",
                                        color = if (record.checkType == CheckType.CHECK_IN) NeonCyan else NeonGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Surface(
                                    color = statusBg,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = statusText,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = dateFormat.format(Date(record.timestamp)),
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        IconButton(onClick = { onDeleteRecord(record.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus Log", tint = TextMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KioskSettingsTab(
    settings: KioskSettings,
    onSaveSettings: (KioskSettings) -> Unit
) {
    var companyName by remember { mutableStateOf(settings.companyName) }
    var address by remember { mutableStateOf(settings.address) }
    var kioskPin by remember { mutableStateOf(settings.kioskPin) }
    var confidenceThreshold by remember { mutableFloatStateOf(settings.faceConfidenceThreshold) }

    // Master Shifts
    var shift1Start by remember { mutableStateOf(settings.shift1Start) }
    var shift1End by remember { mutableStateOf(settings.shift1End) }
    var shift2Start by remember { mutableStateOf(settings.shift2Start) }
    var shift2End by remember { mutableStateOf(settings.shift2End) }
    var shift3Start by remember { mutableStateOf(settings.shift3Start) }
    var shift3End by remember { mutableStateOf(settings.shift3End) }

    // Integrations
    var fonnteApiKey by remember { mutableStateOf(settings.fonnteApiKey) }
    var fonnteAdminWa by remember { mutableStateOf(settings.fonnteAdminWa) }
    var googleSheetsUrl by remember { mutableStateOf(settings.googleSheetsUrl) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Master Shift Setting Card
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = NeonCyan)
                    Text("Master Konfigurasi Shift Kantor (3 Shift)", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Text("Jadwal ini digunakan otomatis oleh sistem untuk karyawan yang disetel mode 'Auto-Detect'.", color = TextSecondary, fontSize = 12.sp)

                // Shift 1
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Shift 1 (Pagi):", color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.width(110.dp), fontSize = 13.sp)
                    OutlinedTextField(
                        value = shift1Start,
                        onValueChange = { shift1Start = it },
                        label = { Text("Mulai") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = CardBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )
                    OutlinedTextField(
                        value = shift1End,
                        onValueChange = { shift1End = it },
                        label = { Text("Selesai") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = CardBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )
                }

                // Shift 2
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Shift 2 (Siang):", color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.width(110.dp), fontSize = 13.sp)
                    OutlinedTextField(
                        value = shift2Start,
                        onValueChange = { shift2Start = it },
                        label = { Text("Mulai") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = CardBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )
                    OutlinedTextField(
                        value = shift2End,
                        onValueChange = { shift2End = it },
                        label = { Text("Selesai") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = CardBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )
                }

                // Shift 3
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Shift 3 (Malam):", color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.width(110.dp), fontSize = 13.sp)
                    OutlinedTextField(
                        value = shift3Start,
                        onValueChange = { shift3Start = it },
                        label = { Text("Mulai") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = CardBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )
                    OutlinedTextField(
                        value = shift3End,
                        onValueChange = { shift3End = it },
                        label = { Text("Selesai") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = CardBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )
                }
            }
        }

        // WhatsApp & Sheets Cloud Sync
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = NeonGreen)
                    Text("Integrasi Notifikasi WhatsApp & Google Sheets", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                OutlinedTextField(
                    value = fonnteApiKey,
                    onValueChange = { fonnteApiKey = it },
                    label = { Text("Token API Fonnte (WhatsApp)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, unfocusedBorderColor = CardBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )

                OutlinedTextField(
                    value = fonnteAdminWa,
                    onValueChange = { fonnteAdminWa = it },
                    label = { Text("Nomor WhatsApp Admin HRD (cth: 081234567890)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, unfocusedBorderColor = CardBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )

                OutlinedTextField(
                    value = googleSheetsUrl,
                    onValueChange = { googleSheetsUrl = it },
                    label = { Text("URL Google Sheets Web App Webhook") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, unfocusedBorderColor = CardBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )

                Surface(
                    color = InputBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Status Sinkronisasi Terakhir: ${settings.lastSheetsSyncStatus}",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        // Kiosk Security
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = NeonYellow)
                    Text("Keamanan & Threshold Wajah", color = NeonYellow, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                OutlinedTextField(
                    value = kioskPin,
                    onValueChange = { kioskPin = it },
                    label = { Text("PIN Kiosk Admin (Untuk Akses Dashboard)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonYellow, unfocusedBorderColor = CardBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )

                Column {
                    Text(
                        text = "Tingkat Kemiripan Wajah (Confidence Threshold): ${(confidenceThreshold * 100).toInt()}%",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = confidenceThreshold,
                        onValueChange = { confidenceThreshold = it },
                        valueRange = 0.50f..0.99f,
                        colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                    )
                    Text("Semakin tinggi threshold, semakin ketat kecocokan biometrik wajah karyawan.", color = TextMuted, fontSize = 11.sp)
                }
            }
        }

        Button(
            onClick = {
                val updated = settings.copy(
                    companyName = companyName.trim(),
                    address = address.trim(),
                    kioskPin = kioskPin.trim(),
                    faceConfidenceThreshold = confidenceThreshold,
                    shift1Start = shift1Start.trim(),
                    shift1End = shift1End.trim(),
                    shift2Start = shift2Start.trim(),
                    shift2End = shift2End.trim(),
                    shift3Start = shift3Start.trim(),
                    shift3End = shift3End.trim(),
                    fonnteApiKey = fonnteApiKey.trim(),
                    fonnteAdminWa = fonnteAdminWa.trim(),
                    googleSheetsUrl = googleSheetsUrl.trim()
                )
                onSaveSettings(updated)
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
        ) {
            Icon(Icons.Default.Save, contentDescription = null, tint = DarkBg)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Simpan Semua Pengaturan", color = DarkBg, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun NotificationsTab(notifications: List<AdminNotification>) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    if (notifications.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.NotificationsNone, contentDescription = null, tint = TextMuted, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Belum ada notifikasi sistem", color = TextSecondary)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(notifications, key = { it.id }) { notif ->
                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = notif.title, fontWeight = FontWeight.Bold, color = if (notif.type == "ALERT") NeonRed else NeonGreen, fontSize = 15.sp)
                            Text(text = dateFormat.format(Date(notif.timestamp)), color = TextMuted, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = notif.message, color = TextPrimary, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
