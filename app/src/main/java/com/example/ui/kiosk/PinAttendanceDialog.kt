package com.example.ui.kiosk

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CheckType
import com.example.ui.theme.*

@Composable
fun PinAttendanceDialog(
    onDismiss: () -> Unit,
    onSubmitPin: (nik: String, pin: String, checkType: CheckType) -> Unit
) {
    var nik by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var selectedCheckType by remember { mutableStateOf(CheckType.CHECK_IN) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
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
                Text(
                    text = "Presensi Manual (PIN)",
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Gunakan jika kamera mengalami kendala",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Check Type Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { selectedCheckType = CheckType.CHECK_IN },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedCheckType == CheckType.CHECK_IN) NeonCyan else InputBg
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Absen Masuk",
                            color = if (selectedCheckType == CheckType.CHECK_IN) DarkBg else TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = { selectedCheckType = CheckType.CHECK_OUT },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedCheckType == CheckType.CHECK_OUT) NeonGreen else InputBg
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Absen Pulang",
                            color = if (selectedCheckType == CheckType.CHECK_OUT) DarkBg else TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = nik,
                    onValueChange = { nik = it },
                    label = { Text("Nomor Induk Karyawan (NIK)") },
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

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6) pin = it },
                    label = { Text("PIN Karyawan") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = NeonCyan) },
                    visualTransformation = PasswordVisualTransformation(),
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

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorMessage!!, color = NeonRed, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Batal", color = TextSecondary)
                    }
                    Button(
                        onClick = {
                            if (nik.isBlank() || pin.isBlank()) {
                                errorMessage = "Mohon isi NIK dan PIN"
                            } else {
                                onSubmitPin(nik.trim(), pin.trim(), selectedCheckType)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Kirim", color = DarkBg, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
