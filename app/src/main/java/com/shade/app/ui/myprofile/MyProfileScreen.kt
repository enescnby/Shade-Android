package com.shade.app.ui.myprofile

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.shade.app.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileScreen(
    onBackClick: () -> Unit,
    viewModel: MyProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme

    var nameText by remember(uiState.displayName) {
        mutableStateOf(uiState.displayName)
    }

    // Fotoğraf kaldırma onay dialogu
    var showRemovePhotoDialog by remember { mutableStateOf(false) }

    // Galeri seçici
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.saveProfilePhoto(uri)
    }

    LaunchedEffect(Unit) {
        viewModel.saveSuccess.collectLatest {
            Toast.makeText(context, "Güncellendi ✓", Toast.LENGTH_SHORT).show()
        }
    }

    // Fotoğraf kaldırma onay dialogu
    if (showRemovePhotoDialog) {
        AlertDialog(
            onDismissRequest = { showRemovePhotoDialog = false },
            containerColor = scheme.surface,
            title = { Text("Fotoğrafı Kaldır", color = scheme.onSurface) },
            text = { Text("Profil fotoğrafını kaldırmak istediğine emin misin?", color = scheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = {
                    showRemovePhotoDialog = false
                    viewModel.removeProfilePhoto()
                }) {
                    Text("Kaldır", color = Color(0xFFFF5252))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemovePhotoDialog = false }) {
                    Text("İptal", color = scheme.onSurfaceVariant)
                }
            }
        )
    }

    Scaffold(
        containerColor = scheme.background,
        topBar = {
            Surface(color = scheme.surface, shadowElevation = 2.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = scheme.onSurface
                        )
                    }
                    Text(
                        "Profilim",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Avatar header ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(AccentPurple.copy(alpha = 0.18f), scheme.background)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    // ── Tıklanabilir avatar ────────────────────────────────────
                    Box(contentAlignment = Alignment.BottomEnd) {

                        // Mor halka
                        Box(
                            modifier = Modifier
                                .size(112.dp)
                                .background(
                                    Brush.linearGradient(listOf(AccentPurple, NeonPurple)),
                                    CircleShape
                                )
                        )

                        // Fotoğraf veya baş harf
                        val photoPath = uiState.profilePhotoPath
                        if (photoPath != null && File(photoPath).exists()) {
                            AsyncImage(
                                model = File(photoPath),
                                contentDescription = "Profil Fotoğrafı",
                                modifier = Modifier
                                    .size(104.dp)
                                    .clip(CircleShape)
                                    .border(3.dp, AccentPurple, CircleShape)
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(
                                                ActivityResultContracts.PickVisualMedia.ImageOnly
                                            )
                                        )
                                    },
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(
                                modifier = Modifier
                                    .size(104.dp)
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(
                                                ActivityResultContracts.PickVisualMedia.ImageOnly
                                            )
                                        )
                                    },
                                shape = CircleShape,
                                color = scheme.surfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    val initial = nameText.ifBlank { uiState.shadeId }
                                        .take(1).uppercase()
                                    Text(
                                        text = initial,
                                        style = MaterialTheme.typography.displaySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentPurple
                                    )
                                }
                            }
                        }

                        // Kamera ikonu rozeti
                        Surface(
                            modifier = Modifier.size(32.dp),
                            shape = CircleShape,
                            color = AccentPurple
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = "Fotoğraf Seç",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(
                                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                                )
                                            )
                                        }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = nameText.ifBlank { "İsim belirlenmedi" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface
                    )

                    // Fotoğraf varsa kaldır linki
                    if (uiState.profilePhotoPath != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Fotoğrafı kaldır",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFF5252),
                            modifier = Modifier.clickable { showRemovePhotoDialog = true }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Shade ID kartı ─────────────────────────────────────────────────
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = scheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, scheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Badge,
                            contentDescription = null,
                            tint = AccentPurple,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Shade ID",
                            style = MaterialTheme.typography.labelMedium,
                            color = AccentPurple,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = uiState.shadeId.ifBlank { "Yükleniyor..." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Bu ID başkalarının seni bulmasını sağlar",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── İsim düzenleme kartı ───────────────────────────────────────────
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = scheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, scheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = AccentPurple,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Görünen Ad",
                            style = MaterialTheme.typography.labelMedium,
                            color = AccentPurple,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Karşı taraf seni kaydetmemişse bu ismi görür",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        placeholder = { Text("Adını gir...", color = scheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPurple,
                            unfocusedBorderColor = scheme.outline,
                            focusedTextColor = scheme.onSurface,
                            unfocusedTextColor = scheme.onSurface,
                            cursorColor = AccentPurple
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { viewModel.saveName(nameText) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = nameText.isNotBlank() && nameText != uiState.displayName,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentPurple,
                            disabledContainerColor = scheme.surfaceContainerHigh
                        )
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kaydet", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Güvenlik bilgisi ───────────────────────────────────────────────
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = scheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, scheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = AccentPurple,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Güvenlik",
                            style = MaterialTheme.typography.labelMedium,
                            color = AccentPurple,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Tüm mesajlar uçtan uca şifreleme (X25519 + ChaCha20) ile korunmaktadır. Sunucu mesajlarını okuyamaz.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
