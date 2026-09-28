package com.example.ui.kiosk

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceType
import com.example.data.model.GestureType
import com.example.data.model.PunctualityStatus
import com.example.ui.theme.AmberCheckout
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.EmeraldSuccessLight
import com.example.ui.theme.KioskCardBackground
import com.example.ui.theme.KioskDarkBackground
import com.example.ui.theme.RoseError
import com.example.ui.theme.PurpleTech

@Composable
fun AttendanceSuccessDialog(
    record: AttendanceRecord,
    onDismiss: () -> Unit
) {
    val isCheckIn = record.type == AttendanceType.CHECK_IN
    val mainColor = if (isCheckIn) EmeraldSuccess else AmberCheckout

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(28.dp))
                .border(2.dp, mainColor.copy(alpha = 0.8f), RoundedCornerShape(28.dp)),
            color = KioskDarkBackground,
            tonalElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Success Badge Header
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(mainColor, mainColor.copy(alpha = 0.3f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isCheckIn) "PRESENSI MASUK BERHASIL" else "PRESENSI PULANG BERHASIL",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = mainColor
                )

                Text(
                    text = "Identitas & Gestur Terverifikasi Secara Real-Time",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Employee Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = KioskCardBackground),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(record.photoSnapshotUri.ifBlank { "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200" })
                                .crossfade(true)
                                .build(),
                            contentDescription = record.employeeName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .border(2.dp, CyanNeon, CircleShape)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = record.employeeName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${record.employeeCode} • ${record.department}",
                                fontSize = 13.sp,
                                color = Color(0xFFCBD5E1)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            // Punctuality Badge
                            val punctualityColor = when (record.punctualityStatus) {
                                PunctualityStatus.ON_TIME -> EmeraldSuccess
                                PunctualityStatus.LATE -> RoseError
                                PunctualityStatus.OVERTIME -> PurpleTech
                                else -> AmberCheckout
                            }
                            val punctualityLabel = when (record.punctualityStatus) {
                                PunctualityStatus.ON_TIME -> "🟢 Tepat Waktu"
                                PunctualityStatus.LATE -> "🔴 Terlambat"
                                PunctualityStatus.EARLY_DEPARTURE -> "🟡 Pulang Awal"
                                PunctualityStatus.OVERTIME -> "🔵 Lembur"
                                else -> "Normal"
                            }
                            Text(
                                text = punctualityLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = punctualityColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Metadata Details: Gesture, Time, Location
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF131D31), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Gesture Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (record.gestureType == GestureType.THUMBS_UP) Icons.Default.ThumbUp else Icons.Default.PanTool,
                                contentDescription = "Gestur",
                                tint = mainColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("Verifikasi Gestur:", fontSize = 13.sp, color = Color(0xFF94A3B8))
                        }
                        Text(
                            text = when (record.gestureType) {
                                GestureType.THUMBS_UP -> "👍 Jempol / SIP (100%)"
                                GestureType.OPEN_HAND -> "🖐️ 5 Jari Terbuka (100%)"
                                GestureType.FACE_ONLY -> "👤 Wajah Terverifikasi"
                                GestureType.MANUAL_PIN -> "🔢 PIN Manual"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Timestamp Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Waktu",
                                tint = CyanNeon,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("Waktu Presensi:", fontSize = 13.sp, color = Color(0xFF94A3B8))
                        }
                        Text(
                            text = "${record.formattedDate}, ${record.formattedTime}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    // Location Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Lokasi",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(18.dp)
                            )
                            Text("Lokasi GPS:", fontSize = 13.sp, color = Color(0xFF94A3B8))
                        }
                        Text(
                            text = record.locationAddress.take(28) + if (record.locationAddress.length > 28) "..." else "",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Cloud Sync & Admin Alert Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldSuccess.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "Cloud",
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Cloud Synced",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldSuccessLight
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = CyanNeon.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Admin Notified",
                                tint = CyanNeon,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Admin Notified",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CyanNeon
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = mainColor)
                ) {
                    Text(
                        text = "Tutup (Otomatis)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}
