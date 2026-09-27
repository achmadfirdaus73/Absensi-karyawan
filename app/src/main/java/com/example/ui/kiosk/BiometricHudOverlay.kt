package com.example.ui.kiosk

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.Employee
import com.example.data.model.GestureType
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldSuccess

@Composable
fun BiometricHudOverlay(
    recognizedEmployee: Employee?,
    activeGesture: GestureType?,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // ONLY the Detected Employee Badge appears when a face is detected (Zero buttons)
        AnimatedVisibility(
            visible = recognizedEmployee != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
        ) {
            if (recognizedEmployee != null) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xF20A1124),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, CyanNeon),
                    shadowElevation = 12.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Employee Info Row (Photo + Name + Department)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, CyanNeon, CircleShape)
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(recognizedEmployee.photoUri.ifBlank { "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100" })
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = recognizedEmployee.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = recognizedEmployee.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Terverifikasi",
                                        tint = EmeraldSuccess,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }

                                Text(
                                    text = "${recognizedEmployee.role} • ${recognizedEmployee.department}",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // Real-time Gestures Guide / Detection Status Badge
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E293B)
                        ) {
                            Text(
                                text = when (activeGesture) {
                                    GestureType.THUMBS_UP -> "👍 Gestur Jempol Terdeteksi • Memproses Masuk..."
                                    GestureType.OPEN_HAND -> "🖐️ Gestur 5 Jari Terdeteksi • Memproses Pulang..."
                                    else -> "Posisikan Wajah • Acungkan 👍 Masuk / 🖐️ Pulang"
                                },
                                color = if (activeGesture != null) EmeraldSuccess else Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = if (activeGesture != null) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
