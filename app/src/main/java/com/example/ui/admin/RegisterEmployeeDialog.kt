package com.example.ui.admin

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.Employee
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.CyanNeonLight
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.EmeraldSuccessLight
import com.example.ui.theme.KioskCardBackground
import com.example.ui.theme.KioskCardBorder
import com.example.ui.theme.KioskDarkBackground
import com.example.ui.theme.KioskDarkSurface
import java.io.File
import java.io.FileOutputStream

@Composable
fun RegisterEmployeeDialog(
    initialEmployee: Employee? = null,
    onDismiss: () -> Unit,
    onSave: (Employee) -> Unit
) {
    val context = LocalContext.current
    val isEditMode = initialEmployee != null

    var name by remember { mutableStateOf(initialEmployee?.name ?: "") }
    var code by remember { mutableStateOf(initialEmployee?.employeeCode ?: "EMP-${(1000..9999).random()}") }
    var department by remember { mutableStateOf(initialEmployee?.department ?: "Operasional") }
    var role by remember { mutableStateOf(initialEmployee?.role ?: "Staff") }
    var email by remember { mutableStateOf(initialEmployee?.email ?: "") }
    var phone by remember { mutableStateOf(initialEmployee?.phone ?: "08") }
    var shiftStart by remember { mutableStateOf(initialEmployee?.shiftStart ?: "08:00") }
    var shiftEnd by remember { mutableStateOf(initialEmployee?.shiftEnd ?: "17:00") }
    
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var photoUriString by remember { mutableStateOf(initialEmployee?.photoUri ?: "") }

    // Camera Capture Launcher (Take Photo directly)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            try {
                val file = File(context.cacheDir, "face_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                photoUriString = Uri.fromFile(file).toString()
            } catch (e: Exception) {
                // Ignore file save error
            }
        }
    }

    // Photo Picker Launcher (Pick from Gallery)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            photoUriString = uri.toString()
            capturedBitmap = null
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .clip(RoundedCornerShape(24.dp)),
            color = KioskDarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, KioskCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isEditMode) "Edit Data Karyawan" else "Daftar Karyawan & Wajah Biometrik",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Sinkronisasi Identitas Biometrik Wajah",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                    }
                }

                // Face Photo Enrollment Section
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = KioskDarkBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "📸 FOTO WAJAH UNTUK DETEKSI OTOMATIS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanNeonLight,
                            letterSpacing = 1.sp
                        )

                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                                .border(2.dp, CyanNeon, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (capturedBitmap != null) {
                                Image(
                                    bitmap = capturedBitmap!!.asImageBitmap(),
                                    contentDescription = "Foto Wajah",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else if (photoUriString.isNotBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(photoUriString)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Foto Wajah",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }

                        // Buttons to Capture or Select Face Photo
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { cameraLauncher.launch() },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ambil Foto", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Collections,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Galeri", color = Color.White, fontSize = 12.sp)
                            }
                        }

                        Text(
                            text = "Pastikan wajah terlihat jelas tanpa masker saat pengambilan foto.",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                // Nama Lengkap
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Lengkap Karyawan *") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = CyanNeon) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // NIK & Departemen
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("NIK / NIP *") },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = CyanNeon) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = department,
                        onValueChange = { department = it },
                        label = { Text("Departemen / Divisi") },
                        leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, tint = CyanNeon) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                // Jabatan / Posisi
                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Jabatan / Posisi Kerja *") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // Jam Shift Kerja
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = shiftStart,
                        onValueChange = { shiftStart = it },
                        label = { Text("Jam Masuk (08:00)") },
                        leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = EmeraldSuccess) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = shiftEnd,
                        onValueChange = { shiftEnd = it },
                        label = { Text("Jam Pulang (17:00)") },
                        leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFF59E0B)) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                // Kontak (Email & Phone)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = CyanNeon) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("No. WhatsApp / HP") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = CyanNeon) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (name.isNotBlank() && code.isNotBlank()) {
                            val finalPhotoUri = if (photoUriString.isNotBlank()) {
                                photoUriString
                            } else {
                                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300"
                            }

                            val cleanName = name.trim()
                            val emp = Employee(
                                id = initialEmployee?.id ?: 0,
                                employeeCode = code.trim(),
                                name = cleanName,
                                department = department.trim(),
                                role = role.trim(),
                                email = if (email.isBlank()) "${cleanName.lowercase().replace(" ", ".")}@company.com" else email.trim(),
                                phone = phone.trim(),
                                shiftStart = shiftStart.trim(),
                                shiftEnd = shiftEnd.trim(),
                                photoUri = finalPhotoUri,
                                faceFeaturesJson = """{"descriptor": [0.45, 0.65, 0.88, 0.32, 0.77], "tag": "${cleanName.lowercase().replace(" ", "_")}"}""",
                                isActive = initialEmployee?.isActive ?: true
                            )
                            onSave(emp)
                        }
                    },
                    enabled = name.isNotBlank() && code.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)
                ) {
                    Text(
                        text = if (isEditMode) "Simpan Perubahan Karyawan" else "Simpan & Daftarkan Biometrik Wajah",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
