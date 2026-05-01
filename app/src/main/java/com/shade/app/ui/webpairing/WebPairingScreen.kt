package com.shade.app.ui.webpairing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shade.app.ui.theme.RichBlack
import com.shade.app.ui.theme.SurfaceDark
import com.shade.app.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebPairingScreen(
    onBackClick: () -> Unit,
    viewModel: WebPairingViewModel = hiltViewModel()
) {
    var isScanning by remember { mutableStateOf(false) }
    val uiState by viewModel.state.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState !is WebPairingUiState.Idle && uiState !is WebPairingUiState.Error) {
            isScanning = false
        }
    }

    Scaffold(
        containerColor = RichBlack,
        topBar = {
            TopAppBar(
                title = { Text("Web'e Bağlan") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isScanning) {
                QrScanner(
                    modifier = Modifier.fillMaxSize(),
                    onQrText = { raw ->
                        isScanning = false
                        viewModel.onQrScanned(raw)
                    }
                )
            } else {
                PairingStateContent(
                    uiState = uiState,
                    onScanClick = {
                        viewModel.reset()
                        isScanning = true
                    },
                    onDisconnect = viewModel::disconnect
                )
            }
        }
    }
}

@Composable
private fun PairingStateContent(
    uiState: WebPairingUiState,
    onScanClick: () -> Unit,
    onDisconnect: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (uiState) {
            is WebPairingUiState.Idle -> {
                Text(
                    "Web uygulamasında görünen QR kodu tara.",
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = onScanClick) { Text("QR Tara") }
            }
            WebPairingUiState.Authorizing -> {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text("Yetkilendiriliyor...", color = TextPrimary)
            }
            WebPairingUiState.Connecting -> {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text("Web'e bağlanılıyor...", color = TextPrimary)
            }
            WebPairingUiState.Connected -> {
                Text(
                    "Web cihazına bağlandın.",
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Oturum 10 dakika sonra otomatik düşer.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = onDisconnect) {
                    Text("Bağlantıyı sonlandır")
                }
            }
            is WebPairingUiState.Error -> {
                Text(
                    uiState.message,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = onScanClick) { Text("Tekrar Dene") }
            }
        }
    }
}
