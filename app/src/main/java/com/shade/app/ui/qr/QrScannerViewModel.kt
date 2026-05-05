package com.shade.app.ui.qr

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shade.app.data.remote.api.MessageService
import com.shade.app.security.KeyVaultManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class QrScannerUiState {
    object Idle    : QrScannerUiState()
    object Loading : QrScannerUiState()
    object Success : QrScannerUiState()
    data class Error(val message: String) : QrScannerUiState()
}

@HiltViewModel
class QrScannerViewModel @Inject constructor(
    private val keyVaultManager: KeyVaultManager,
    private val messageService: MessageService   // web-link endpoint için — ileride ayrı servis yapılabilir
) : ViewModel() {

    private val _uiState = MutableStateFlow<QrScannerUiState>(QrScannerUiState.Idle)
    val uiState = _uiState.asStateFlow()

    /**
     * QR içeriğini işle:
     * Format: "shade-web:<sessionToken>" bekleniyor
     * Örnek: "shade-web:abc123xyz"
     */
    fun processScannedQr(content: String) {
        Log.d("QrScanner", "Taranan QR: $content")

        if (!content.startsWith("shade-web:")) {
            _uiState.value = QrScannerUiState.Error(
                "Geçersiz QR kodu.\nBu bir Shade Web QR'ı değil."
            )
            return
        }

        val sessionToken = content.removePrefix("shade-web:").trim()
        if (sessionToken.isBlank()) {
            _uiState.value = QrScannerUiState.Error("QR kodu boş bir oturum içeriyor.")
            return
        }

        _uiState.value = QrScannerUiState.Loading
        viewModelScope.launch {
            try {
                val token = keyVaultManager.getAccessToken() ?: run {
                    _uiState.value = QrScannerUiState.Error("Oturum bulunamadı. Lütfen tekrar giriş yap.")
                    return@launch
                }
                // Backend'e web session linki gönder
                val response = messageService.linkWebSession("Bearer $token", sessionToken)
                if (response.isSuccessful) {
                    _uiState.value = QrScannerUiState.Success
                    Log.d("QrScanner", "Web oturumu bağlandı ✓")
                } else {
                    _uiState.value = QrScannerUiState.Error(
                        "Sunucu hatası: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                Log.e("QrScanner", "Web link hatası: ${e.message}")
                _uiState.value = QrScannerUiState.Error(
                    "Bağlantı hatası: ${e.message?.take(60)}"
                )
            }
        }
    }

    fun reset() {
        _uiState.value = QrScannerUiState.Idle
    }
}
