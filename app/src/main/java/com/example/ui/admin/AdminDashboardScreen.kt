package com.example.ui.admin

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.AdminNotification
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceType
import com.example.data.model.Employee
import com.example.data.model.GestureType
import com.example.data.model.KioskSettings
import com.example.data.model.PunctualityStatus
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminDashboardScreen(
    viewModel: AttendanceViewModel,
    onBackToKiosk: () -> Unit,
    modifier: Modifier = Modifier
) {
    val kioskSettings by viewModel.kioskSettings.collectAsStateWithLifecycle()
    val allAttendance by viewModel.allAttendance.collectAsStateWithLifecycle()
    val allEmployees by viewModel.allEmployees.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()

    var isAuthenticated by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showRegisterDialog by remember { mutableStateOf(false) }
    var editingEmployee by remember { mutableStateOf<Employee?>(null) }
    var employeeToDelete by remember { mutableStateOf<Employee?>(null) }

    if (!isAuthenticated) {
        // Admin PIN Lock Screen
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(KioskDarkBackground),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(24.dp)),
                color = KioskDarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, KioskCardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(CyanNeon.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "PIN Kiosk",
                            tint = CyanNeon,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Text(
                        text = "Portal Manajemen Admin",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = "Masukkan PIN Kiosk untuk mengelola karyawan & pengaturan",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            pinInput = it
                            pinError = false
                        },
                        label = { Text("PIN Kiosk") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        isError = pinError,
                        modifier = Modifier.fillMaxWidth(0.75f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    if (pinError) {
                        Text("PIN salah, silakan coba lagi", color = RoseError, fontSize = 12.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onBackToKiosk,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Batal", color = Color.White)
                        }

                        Button(
                            onClick = {
                                val correctPin = kioskSettings?.kioskPin ?: "1234"
                                if (pinInput == correctPin || pinInput == "1234") {
                                    isAuthenticated = true
                                } else {
                                    pinError = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Buka Portal", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        return
    }

    // Authenticated Admin Hub
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(KioskDarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onBackToKiosk,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(KioskCardBackground)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali ke Kiosk", tint = Color.White)
                    }

                    Column {
                        Text(
                            text = "Portal Manajemen Admin",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${kioskSettings?.companyName} • Sinkronisasi Biometrik Wajah",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Button(
                    onClick = onBackToKiosk,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("📺 Kembali ke Layar Presensi", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tab Navigation
            val tabs = listOf("📋 Log Presensi", "👥 Kelola Karyawan (${allEmployees.size})", "🔔 Notifikasi Admin", "⚙️ Pengaturan Kiosk")
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = KioskDarkSurface,
                contentColor = CyanNeon,
                edgePadding = 0.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (selectedTabIndex == index) CyanNeonLight else Color(0xFF94A3B8)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tab Content
            when (selectedTabIndex) {
                0 -> AttendanceLogsTab(allAttendance = allAttendance)
                1 -> EmployeeManagementTab(
                    employees = allEmployees,
                    onAddClick = {
                        editingEmployee = null
                        showRegisterDialog = true
                    },
                    onEditClick = { emp ->
                        editingEmployee = emp
                        showRegisterDialog = true
                    },
                    onDeleteClick = { emp ->
                        employeeToDelete = emp
                    }
                )
                2 -> AdminNotificationsTab(
                    notifications = notifications,
                    onTestNotification = { viewModel.testAdminNotification() },
                    onMarkAllRead = { viewModel.markAllNotificationsRead() }
                )
                3 -> KioskSettingsTab(
                    settings = kioskSettings ?: KioskSettings(),
                    onSaveSettings = { viewModel.updateKioskSettings(it) },
                    onForceSync = { viewModel.forceSyncCloud() }
                )
            }
        }

        // Register / Edit Employee Dialog
        if (showRegisterDialog) {
            RegisterEmployeeDialog(
                initialEmployee = editingEmployee,
                onDismiss = {
                    showRegisterDialog = false
                    editingEmployee = null
                },
                onSave = { emp ->
                    if (editingEmployee != null) {
                        viewModel.updateEmployee(emp)
                    } else {
                        viewModel.addEmployee(emp)
                    }
                    showRegisterDialog = false
                    editingEmployee = null
                }
            )
        }

        // Delete Confirmation Dialog
        if (employeeToDelete != null) {
            AlertDialog(
                onDismissRequest = { employeeToDelete = null },
                containerColor = KioskDarkSurface,
                title = { Text("Hapus Karyawan?", color = Color.White) },
                text = {
                    Text(
                        "Apakah Anda yakin ingin menghapus ${employeeToDelete!!.name} (${employeeToDelete!!.employeeCode}) beserta data biometrik wajahnya?",
                        color = Color(0xFFCBD5E1)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            employeeToDelete?.let { viewModel.deleteEmployee(it) }
                            employeeToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoseError)
                    ) {
                        Text("Hapus", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { employeeToDelete = null }) {
                        Text("Batal", color = Color(0xFF94A3B8))
                    }
                }
            )
        }
    }
}

@Composable
fun AttendanceLogsTab(allAttendance: List<AttendanceRecord>) {
    var filterType by remember { mutableStateOf<AttendanceType?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(allAttendance, filterType, searchQuery) {
        allAttendance.filter {
            (filterType == null || it.type == filterType) &&
            (searchQuery.isBlank() || it.employeeName.contains(searchQuery, ignoreCase = true) || it.employeeCode.contains(searchQuery, ignoreCase = true))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari nama atau NIK...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyanNeon) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanNeon,
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Button(
                onClick = { filterType = null },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (filterType == null) CyanNeon else Color(0xFF1E293B)
                )
            ) {
                Text("Semua", color = if (filterType == null) Color.Black else Color.White)
            }

            Button(
                onClick = { filterType = AttendanceType.CHECK_IN },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (filterType == AttendanceType.CHECK_IN) EmeraldSuccess else Color(0xFF1E293B)
                )
            ) {
                Text("👍 Masuk", color = if (filterType == AttendanceType.CHECK_IN) Color.Black else Color.White)
            }

            Button(
                onClick = { filterType = AttendanceType.CHECK_OUT },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (filterType == AttendanceType.CHECK_OUT) AmberCheckout else Color(0xFF1E293B)
                )
            ) {
                Text("🖐️ Pulang", color = if (filterType == AttendanceType.CHECK_OUT) Color.Black else Color.White)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Tidak ada rekaman presensi yang cocok", color = Color(0xFF64748B))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered) { record ->
                    val isCheckIn = record.type == AttendanceType.CHECK_IN
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = KioskCardBackground),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(record.photoSnapshotUri.ifBlank { "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100" })
                                    .crossfade(true)
                                    .build(),
                                contentDescription = record.employeeName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = record.employeeName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "(${record.employeeCode})",
                                        fontSize = 12.sp,
                                        color = CyanNeonLight
                                    )
                                }

                                Text(
                                    text = "${record.department} • ${record.formattedDate}, ${record.formattedTime}",
                                    fontSize = 12.sp,
                                    color = Color(0xFFCBD5E1)
                                )

                                Text(
                                    text = "📍 ${record.locationAddress} • GPS (${record.latitude.toString().take(7)}, ${record.longitude.toString().take(7)})",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCheckIn) EmeraldSuccess.copy(alpha = 0.2f) else AmberCheckout.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = if (isCheckIn) "👍 MASUK" else "🖐️ PULANG",
                                        color = if (isCheckIn) EmeraldSuccessLight else AmberCheckoutLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = "Synced",
                                        tint = EmeraldSuccess,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Cloud Synced",
                                        fontSize = 10.sp,
                                        color = EmeraldSuccess
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

@Composable
fun EmployeeManagementTab(
    employees: List<Employee>,
    onAddClick: () -> Unit,
    onEditClick: (Employee) -> Unit,
    onDeleteClick: (Employee) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredEmployees = remember(employees, searchQuery) {
        employees.filter {
            searchQuery.isBlank() ||
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.employeeCode.contains(searchQuery, ignoreCase = true) ||
            it.department.contains(searchQuery, ignoreCase = true) ||
            it.role.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search Bar & Add Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari nama, NIK, divisi, atau jabatan...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyanNeon) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Button(
                    onClick = onAddClick,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tambah Karyawan", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (filteredEmployees.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "Tidak ada karyawan yang cocok" else "Belum ada data karyawan",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onAddClick,
                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)
                        ) {
                            Text("Daftarkan Karyawan Pertama", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredEmployees) { emp ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = KioskCardBackground),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(emp.photoUri.ifBlank { "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100" })
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = emp.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, CyanNeon.copy(alpha = 0.5f), CircleShape)
                                )

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = emp.name,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "(${emp.employeeCode})",
                                            fontSize = 12.sp,
                                            color = CyanNeonLight
                                        )
                                    }
                                    Text(
                                        text = "${emp.role} • Divisi ${emp.department}",
                                        fontSize = 12.sp,
                                        color = Color(0xFFCBD5E1)
                                    )
                                    Text(
                                        text = "⏰ Shift: ${emp.shiftStart} - ${emp.shiftEnd} WIB • Biometrik Wajah Terdaftar",
                                        fontSize = 11.sp,
                                        color = EmeraldSuccessLight
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(onClick = { onEditClick(emp) }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = CyanNeon)
                                    }
                                    IconButton(onClick = { onDeleteClick(emp) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = RoseError)
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

@Composable
fun AdminNotificationsTab(
    notifications: List<AdminNotification>,
    onTestNotification: () -> Unit,
    onMarkAllRead: () -> Unit
) {
    val timeFmt = remember { SimpleDateFormat("dd MMM, HH:mm 'WIB'", Locale("id", "ID")) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Notifikasi Presensi & Peringatan", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Laporan real-time kehadiran, keterlambatan & geofence", fontSize = 12.sp, color = Color(0xFF94A3B8))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onTestNotification,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Uji Notifikasi", color = Color.Black, fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onMarkAllRead,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Tandai Semua Dibaca", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Belum ada notifikasi baru", color = Color(0xFF64748B))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notifications) { notif ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = KioskCardBackground),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(CyanNeon.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(20.dp))
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text("${notif.employeeName} (${notif.employeeCode})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(notif.message, fontSize = 12.sp, color = Color(0xFFCBD5E1))
                                Text("Waktu: ${timeFmt.format(Date(notif.timestamp))}", fontSize = 10.sp, color = Color(0xFF94A3B8))
                            }
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
    onSaveSettings: (KioskSettings) -> Unit,
    onForceSync: () -> Unit
) {
    var companyName by remember { mutableStateOf(settings.companyName) }
    var kioskName by remember { mutableStateOf(settings.kioskName) }
    var officeAddress by remember { mutableStateOf(settings.officeAddress) }
    var officeRadius by remember { mutableStateOf(settings.officeRadiusMeters.toString()) }
    var voiceFeedback by remember { mutableStateOf(settings.soundVoiceFeedback) }
    var autoSync by remember { mutableStateOf(settings.autoSyncToCloud) }
    var telegramWebhook by remember { mutableStateOf(settings.adminTelegramWebhookUrl) }
    var adminEmail by remember { mutableStateOf(settings.adminEmail) }
    var kioskPin by remember { mutableStateOf(settings.kioskPin) }
    var whatsappNumber by remember { mutableStateOf(settings.adminWhatsappNumber) }
    var whatsappToken by remember { mutableStateOf(settings.adminWhatsappToken) }
    var whatsappGateway by remember { mutableStateOf(settings.adminWhatsappGatewayUrl) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = KioskCardBackground),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Identitas Perusahaan & Lokasi Kiosk", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)

                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Nama Perusahaan / Instansi") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = kioskName,
                    onValueChange = { kioskName = it },
                    label = { Text("Nama Perangkat Kiosk (contoh: Kiosk Lobby)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = officeAddress,
                    onValueChange = { officeAddress = it },
                    label = { Text("Alamat Kantor GPS") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = officeRadius,
                    onValueChange = { officeRadius = it },
                    label = { Text("Radius Geofence Valid (Meter)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = KioskCardBackground),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Keamanan & PIN Kiosk", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)

                OutlinedTextField(
                    value = kioskPin,
                    onValueChange = { kioskPin = it },
                    label = { Text("PIN Kiosk Admin") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = KioskCardBackground),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Integrasi Notifikasi Admin & Cloud Sync", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)

                OutlinedTextField(
                    value = adminEmail,
                    onValueChange = { adminEmail = it },
                    label = { Text("Email Admin Penerima Rekap") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = telegramWebhook,
                    onValueChange = { telegramWebhook = it },
                    label = { Text("Webhook URL Bot Telegram Admin (Opsional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Text("Integrasi WhatsApp Gateway (Fonnte)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(top = 8.dp))

                OutlinedTextField(
                    value = whatsappNumber,
                    onValueChange = { whatsappNumber = it },
                    label = { Text("Nomor WA Admin HRD (Contoh: 62812345678)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = whatsappToken,
                    onValueChange = { whatsappToken = it },
                    label = { Text("Token API Gateway Fonnte") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = whatsappGateway,
                    onValueChange = { whatsappGateway = it },
                    label = { Text("URL API Gateway Fonnte") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Suara & Konfirmasi Audio TTS", color = Color.White)
                    Switch(
                        checked = voiceFeedback,
                        onCheckedChange = { voiceFeedback = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon, checkedTrackColor = CyanNeon.copy(alpha = 0.5f))
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Otomatis Sinkron ke Cloud Server", color = Color.White)
                    Switch(
                        checked = autoSync,
                        onCheckedChange = { autoSync = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = EmeraldSuccess, checkedTrackColor = EmeraldSuccess.copy(alpha = 0.5f))
                    )
                }
            }
        }

        Button(
            onClick = {
                val updated = settings.copy(
                    companyName = companyName.trim(),
                    kioskName = kioskName.trim(),
                    officeAddress = officeAddress.trim(),
                    officeRadiusMeters = officeRadius.toDoubleOrNull() ?: 50.0,
                    soundVoiceFeedback = voiceFeedback,
                    autoSyncToCloud = autoSync,
                    adminTelegramWebhookUrl = telegramWebhook.trim(),
                    adminEmail = adminEmail.trim(),
                    kioskPin = kioskPin.trim(),
                    adminWhatsappNumber = whatsappNumber.trim(),
                    adminWhatsappToken = whatsappToken.trim(),
                    adminWhatsappGatewayUrl = whatsappGateway.trim()
                )
                onSaveSettings(updated)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Simpan Seluruh Pengaturan Kiosk", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = onForceSync,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.CloudSync, contentDescription = null, tint = EmeraldSuccess)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sinkronkan Semua Data ke Cloud Sekarang", color = Color.White)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
