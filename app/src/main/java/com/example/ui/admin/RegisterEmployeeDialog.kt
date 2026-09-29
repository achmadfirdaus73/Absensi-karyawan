package com.example.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Employee
import com.example.data.model.KioskSettings
import com.example.data.model.ShiftAssignmentType
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterEmployeeDialog(
    employeeToEdit: Employee? = null,
    kioskSettings: KioskSettings,
    onDismiss: () -> Unit,
    onSaveEmployee: (Employee) -> Unit
) {
    var name by remember { mutableStateOf(employeeToEdit?.name ?: "") }
    var nik by remember { mutableStateOf(employeeToEdit?.nik ?: "") }
    var division by remember { mutableStateOf(employeeToEdit?.division ?: "Operasional") }
    var role by remember { mutableStateOf(employeeToEdit?.role ?: "Staff") }
    var customRoleText by remember { mutableStateOf("") }
    
    // Shift selection
    var selectedShiftType by remember {
        mutableStateOf(
            try {
                ShiftAssignmentType.valueOf(employeeToEdit?.shiftType ?: ShiftAssignmentType.AUTO_DETECT.name)
            } catch (_: Exception) {
                ShiftAssignmentType.AUTO_DETECT
            }
        )
    }
    var customStartTime by remember { mutableStateOf(employeeToEdit?.customStartTime ?: "09:00") }
    var customEndTime by remember { mutableStateOf(employeeToEdit?.customEndTime ?: "18:00") }
    var pin by remember { mutableStateOf(employeeToEdit?.pin ?: "1234") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var isShiftDropdownExpanded by remember { mutableStateOf(false) }
    var isRoleDropdownExpanded by remember { mutableStateOf(false) }

    val standardRoles = listOf("Manager", "Supervisor", "Staff", "Security", "Cleaning Service", "Admin", "Lainnya...")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.92f)
                .border(1.5.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (employeeToEdit == null) "Daftarkan Karyawan Baru" else "Edit Data Karyawan",
                            style = MaterialTheme.typography.titleLarge,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Konfigurasikan profil, role jabatan, dan jam kerja",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = CardBorder)
                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Form
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Nama Lengkap
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Lengkap Karyawan") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = NeonCyan) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    // 2. NIK & Divisi
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = nik,
                            onValueChange = { nik = it },
                            label = { Text("NIK / ID") },
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = NeonCyan) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        OutlinedTextField(
                            value = division,
                            onValueChange = { division = it },
                            label = { Text("Divisi / Departemen") },
                            leadingIcon = { Icon(Icons.Default.CorporateFare, contentDescription = null, tint = NeonCyan) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }

                    // 3. Role / Jabatan (Dropdown)
                    Column {
                        Text(
                            text = "Jabatan / Role Karyawan",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        ExposedDropdownMenuBox(
                            expanded = isRoleDropdownExpanded,
                            onExpandedChange = { isRoleDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = role,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Pilih Role") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRoleDropdownExpanded) },
                                leadingIcon = { Icon(Icons.Default.Work, contentDescription = null, tint = NeonPurple) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonPurple,
                                    unfocusedBorderColor = CardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            ExposedDropdownMenu(
                                expanded = isRoleDropdownExpanded,
                                onDismissRequest = { isRoleDropdownExpanded = false },
                                modifier = Modifier.background(CardBg)
                            ) {
                                standardRoles.forEach { r ->
                                    DropdownMenuItem(
                                        text = { Text(r, color = TextPrimary) },
                                        onClick = {
                                            if (r == "Lainnya...") {
                                                role = "Lainnya"
                                            } else {
                                                role = r
                                            }
                                            isRoleDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (role == "Lainnya") {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customRoleText,
                                onValueChange = { customRoleText = it },
                                label = { Text("Ketik Nama Jabatan Khusus") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonPurple,
                                    unfocusedBorderColor = CardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                        }
                    }

                    // 4. Pengaturan Jam Kerja / Shift (DROPDOWN FITUR UTAMA)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = InputBg)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = NeonCyan)
                                Text(
                                    text = "Konfigurasi Waktu Kerja & Shift",
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tentukan apakah karyawan mengikuti shift otomatis kantor atau jadwal khusus role",
                                color = TextMuted,
                                fontSize = 12.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Shift Dropdown Selector
                            ExposedDropdownMenuBox(
                                expanded = isShiftDropdownExpanded,
                                onExpandedChange = { isShiftDropdownExpanded = it }
                            ) {
                                val currentShiftLabel = when (selectedShiftType) {
                                    ShiftAssignmentType.AUTO_DETECT -> "🔄 Sesuai Shift Kantor (Auto-Detect)"
                                    ShiftAssignmentType.SHIFT_1 -> "☀️ Shift 1 Tetap (${kioskSettings.shift1Start} - ${kioskSettings.shift1End})"
                                    ShiftAssignmentType.SHIFT_2 -> "⛅ Shift 2 Tetap (${kioskSettings.shift2Start} - ${kioskSettings.shift2End})"
                                    ShiftAssignmentType.SHIFT_3 -> "🌙 Shift 3 Tetap (${kioskSettings.shift3Start} - ${kioskSettings.shift3End})"
                                    ShiftAssignmentType.MANUAL_CUSTOM -> "✍️ Masukkan Jam Kerja Khusus (Manual)"
                                }

                                OutlinedTextField(
                                    value = currentShiftLabel,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Mode Waktu Kerja") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isShiftDropdownExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NeonCyan,
                                        unfocusedBorderColor = CardBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )

                                ExposedDropdownMenu(
                                    expanded = isShiftDropdownExpanded,
                                    onDismissRequest = { isShiftDropdownExpanded = false },
                                    modifier = Modifier.background(CardBg)
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text("🔄 Sesuai Shift Kantor (Auto-Detect)", color = NeonCyan, fontWeight = FontWeight.Bold)
                                                Text("Otomatis mendeteksi Shift 1, 2, atau 3 kantor", color = TextSecondary, fontSize = 11.sp)
                                            }
                                        },
                                        onClick = {
                                            selectedShiftType = ShiftAssignmentType.AUTO_DETECT
                                            isShiftDropdownExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text("☀️ Shift 1 Tetap (Pagi)", color = TextPrimary, fontWeight = FontWeight.Bold)
                                                Text("Jam ${kioskSettings.shift1Start} s/d ${kioskSettings.shift1End}", color = TextSecondary, fontSize = 11.sp)
                                            }
                                        },
                                        onClick = {
                                            selectedShiftType = ShiftAssignmentType.SHIFT_1
                                            isShiftDropdownExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text("⛅ Shift 2 Tetap (Siang/Sore)", color = TextPrimary, fontWeight = FontWeight.Bold)
                                                Text("Jam ${kioskSettings.shift2Start} s/d ${kioskSettings.shift2End}", color = TextSecondary, fontSize = 11.sp)
                                            }
                                        },
                                        onClick = {
                                            selectedShiftType = ShiftAssignmentType.SHIFT_2
                                            isShiftDropdownExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text("🌙 Shift 3 Tetap (Malam)", color = TextPrimary, fontWeight = FontWeight.Bold)
                                                Text("Jam ${kioskSettings.shift3Start} s/d ${kioskSettings.shift3End}", color = TextSecondary, fontSize = 11.sp)
                                            }
                                        },
                                        onClick = {
                                            selectedShiftType = ShiftAssignmentType.SHIFT_3
                                            isShiftDropdownExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text("✍️ Masukkan Jam Khusus (Manual)", color = NeonYellow, fontWeight = FontWeight.Bold)
                                                Text("Tentukan jam masuk & pulang fleksibel (misal Manager)", color = TextSecondary, fontSize = 11.sp)
                                            }
                                        },
                                        onClick = {
                                            selectedShiftType = ShiftAssignmentType.MANUAL_CUSTOM
                                            isShiftDropdownExpanded = false
                                        }
                                    )
                                }
                            }

                            // If MANUAL_CUSTOM is selected, show custom time inputs!
                            if (selectedShiftType == ShiftAssignmentType.MANUAL_CUSTOM) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = customStartTime,
                                        onValueChange = { customStartTime = it },
                                        label = { Text("Jam Masuk (HH:mm)") },
                                        placeholder = { Text("09:00") },
                                        leadingIcon = { Icon(Icons.Default.Login, contentDescription = null, tint = NeonGreen) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = NeonGreen,
                                            unfocusedBorderColor = CardBorder,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        )
                                    )

                                    OutlinedTextField(
                                        value = customEndTime,
                                        onValueChange = { customEndTime = it },
                                        label = { Text("Jam Pulang (HH:mm)") },
                                        placeholder = { Text("18:00") },
                                        leadingIcon = { Icon(Icons.Default.Logout, contentDescription = null, tint = NeonYellow) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = NeonYellow,
                                            unfocusedBorderColor = CardBorder,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "💡 Contoh: Manager masuk 09:00 pulang 18:00. Toleransi telat 15 menit.",
                                    color = NeonYellow.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // 5. PIN Cadangan Karyawan
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 6) pin = it },
                        label = { Text("PIN Karyawan (Untuk Presensi Manual)") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = NeonCyan) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    // 6. Biometrik Info Box
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = NeonCyan.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Face, contentDescription = null, tint = NeonCyan)
                            Column {
                                Text(
                                    text = "Pendaftaran Wajah Biometrik",
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Wajah akan otomatis terdeteksi dan terdaftar saat karyawan melakukan presensi perdana di kamera Kiosk.",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = NeonRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = CardBorder)
                Spacer(modifier = Modifier.height(16.dp))

                // Actions Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Batal", color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            val finalRole = if (role == "Lainnya") {
                                if (customRoleText.isNotBlank()) customRoleText.trim() else "Karyawan"
                            } else {
                                role
                            }

                            if (name.isBlank() || nik.isBlank()) {
                                errorMessage = "Mohon isi Nama Lengkap dan NIK"
                            } else {
                                val employee = (employeeToEdit ?: Employee(name = "", nik = "", division = "")).copy(
                                    name = name.trim(),
                                    nik = nik.trim(),
                                    division = division.trim(),
                                    role = finalRole,
                                    shiftType = selectedShiftType.name,
                                    customStartTime = customStartTime.trim(),
                                    customEndTime = customEndTime.trim(),
                                    pin = pin.trim()
                                )
                                onSaveEmployee(employee)
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = DarkBg)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simpan Karyawan", color = DarkBg, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
