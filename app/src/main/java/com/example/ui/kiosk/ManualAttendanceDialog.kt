package com.example.ui.kiosk

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.AttendanceType
import com.example.data.model.Employee
import com.example.ui.theme.AmberCheckout
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.KioskCardBackground
import com.example.ui.theme.KioskDarkBackground

@Composable
fun ManualAttendanceDialog(
    employees: List<Employee>,
    onConfirm: (Employee, AttendanceType) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedEmployee by remember { mutableStateOf<Employee?>(null) }
    var selectedType by remember { mutableStateOf(AttendanceType.CHECK_IN) }

    val filteredEmployees = remember(searchQuery, employees) {
        if (searchQuery.isBlank()) employees
        else employees.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.employeeCode.contains(searchQuery, ignoreCase = true) ||
            it.department.contains(searchQuery, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp)),
            color = KioskDarkBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Presensi Cadangan Manual",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Pilih karyawan & jenis presensi",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari nama atau NIK...", color = Color(0xFF64748B)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyanNeon) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Mode Selection Masuk vs Pulang
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { selectedType = AttendanceType.CHECK_IN },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == AttendanceType.CHECK_IN) EmeraldSuccess else Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "👍 Absen Masuk",
                            color = if (selectedType == AttendanceType.CHECK_IN) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { selectedType = AttendanceType.CHECK_OUT },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == AttendanceType.CHECK_OUT) AmberCheckout else Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "🖐️ Absen Pulang",
                            color = if (selectedType == AttendanceType.CHECK_OUT) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Employee List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredEmployees) { emp ->
                        val isSelected = selectedEmployee?.id == emp.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedEmployee = emp },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) CyanNeon.copy(alpha = 0.2f) else KioskCardBackground,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, CyanNeon) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
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
                                        .size(42.dp)
                                        .clip(CircleShape)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = emp.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${emp.employeeCode} • ${emp.department}",
                                        fontSize = 12.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Submit Button
                Button(
                    onClick = {
                        selectedEmployee?.let { onConfirm(it, selectedType) }
                    },
                    enabled = selectedEmployee != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedType == AttendanceType.CHECK_IN) EmeraldSuccess else AmberCheckout,
                        disabledContainerColor = Color(0xFF334155)
                    )
                ) {
                    Text(
                        text = "Konfirmasi Presensi (${if (selectedType == AttendanceType.CHECK_IN) "Masuk" else "Pulang"})",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}
