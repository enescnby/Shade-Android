package com.shade.app.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shade.app.data.local.DatabaseSeeder
import com.shade.app.data.preferences.ThemeMode
import com.shade.app.data.preferences.ThemePreferenceRepository
import com.shade.app.security.KeyVaultManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val keyVaultManager: KeyVaultManager,
    private val themePreferenceRepository: ThemePreferenceRepository,
    private val databaseSeeder: DatabaseSeeder
) : ViewModel() {

    private val _loggedOut = MutableStateFlow(false)
    val loggedOut: StateFlow<Boolean> = _loggedOut.asStateFlow()

    val themeMode: StateFlow<ThemeMode> = themePreferenceRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch(Dispatchers.IO) {
            themePreferenceRepository.setThemeMode(mode)
        }
    }

    private val _seedStatus = MutableStateFlow<String?>(null)
    val seedStatus: StateFlow<String?> = _seedStatus.asStateFlow()

    fun seedDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            _seedStatus.value = "Oluşturuluyor..."
            try {
                val myShadeId = keyVaultManager.getShadeId() ?: "ME-0000-0000"
                databaseSeeder.seed(myShadeId)
                _seedStatus.value = "✅ 7 kişi ve mesajlar eklendi!"
            } catch (e: Exception) {
                _seedStatus.value = "❌ Hata: ${e.message}"
                Log.e(TAG, "Seed hatası", e)
            }
        }
    }

    fun clearSeedStatus() { _seedStatus.value = null }

    fun logout() {
        Log.d(TAG, "Çıkış yapılıyor...")
        viewModelScope.launch {
            withContext(Dispatchers.IO) { keyVaultManager.clearVault() }
            _loggedOut.value = true
        }
    }

    private companion object {
        const val TAG = "SHADE_SETTINGS"
    }
}
