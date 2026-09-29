package com.example.ui.kiosk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.CheckType
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AttendanceSuccessDialog(
    record: AttendanceRecord,
    onDismiss: () -> Unit
) {
    val isCheckIn = record.checkType == CheckType.CHECK_IN
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
    val recordDate = Date(record.timestamp)

    val (statusText, statusBg, statusTextColor) = when (record.status) {
        AttendanceStatus.ON_TIME -> Triple("TEPAT WAKTU", NeonGreen.copy(alpha = 0.2f), NeonGreen)
        AttendanceStatus.LATE -> Triple("TERLAMBAT", NeonRed.copy(alpha = 0.2f), NeonRed)
        AttendanceStatus.EARLY_DEPARTURE -> Triple("PULANG AWAL", NeonYellow.copy(alpha = 0.2f), NeonYellow)
        AttendanceStatus.OVERTIME -> Triple("LEMBUR", NeonPurple.copy(alpha = 0.2f), NeonPurple)
    }

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
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(NeonGreen.copy(alpha = 0.2f), CircleShape)
                        .border(2.dp, NeonGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = NeonGreen,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isCheckIn) "ABSEN MASUK BERHASIL!" else "ABSEN PULANG BERHASIL!",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isCheckIn) NeonCyan else NeonGreen,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = dateFormat.format(recordDate),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Employee Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = InputBg)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = record.employeeName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = NeonPurple.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = record.role,
                                    color = NeonPurple,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "NIK: ${record.nik}",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = CardBorder)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Jam Presensi", color = TextSecondary, fontSize = 12.sp)
                                Text(
                                    text = timeFormat.format(recordDate),
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Status Kerja", color = TextSecondary, fontSize = 12.sp)
                                Surface(
                                    color = statusBg,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = statusText,
                                        color = statusTextColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Shift / Jadwal", color = TextSecondary, fontSize = 12.sp)
                            Text(
                                text = record.shiftName,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text(text = "Tutup", color = DarkBg, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
