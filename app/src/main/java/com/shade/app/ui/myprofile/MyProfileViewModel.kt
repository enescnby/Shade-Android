package com.shade.app.ui.myprofile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shade.app.data.remote.api.UserService
import com.shade.app.data.remote.dto.UpdateDisplayNameRequest
import com.shade.app.data.repository.AppPrefsRepository
import com.shade.app.security.KeyVaultManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class MyProfileUiState(
    val shadeId: String = "",
    val displayName: String = "",
    val profilePhotoPath: String? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class MyProfileViewModel @Inject constructor(
    private val keyVaultManager: KeyVaultManager,
    private val appPrefsRepository: AppPrefsRepository,
    private val userService: UserService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyProfileUiState())
    val uiState = _uiState.asStateFlow()

    private val _saveSuccess = MutableSharedFlow<Unit>()
    val saveSuccess = _saveSuccess.asSharedFlow()

    init {
        viewModelScope.launch {
            val shadeId = keyVaultManager.getShadeId() ?: ""
            _uiState.update { it.copy(shadeId = shadeId, isLoading = false) }
        }
        viewModelScope.launch {
            appPrefsRepository.displayName.collect { name ->
                _uiState.update { it.copy(displayName = name) }
            }
        }
        viewModelScope.launch {
            appPrefsRepository.profilePhotoPath.collect { path ->
                _uiState.update { it.copy(profilePhotoPath = path) }
            }
        }
    }

    fun saveName(name: String) {
        if (name.isBlank()) return
        val trimmed = name.trim()
        viewModelScope.launch {
            // 1. Önce yerel olarak kaydet
            appPrefsRepository.setDisplayName(trimmed)

            // 2. Backend'e de gönder (başarısız olsa bile yerel kayıt tutulur)
            try {
                val token = keyVaultManager.getAccessToken() ?: return@launch
                userService.updateDisplayName(
                    "Bearer $token",
                    UpdateDisplayNameRequest(trimmed)
                )
            } catch (e: Exception) {
                android.util.Log.w("MyProfile", "İsim sunucuya gönderilemedi: ${e.message}")
            }

            _saveSuccess.emit(Unit)
        }
    }

    /** Galeri'den gelen URI'yi uygulamanın dosya dizinine kopyalar ve path'i kaydeder. */
    fun saveProfilePhoto(uri: Uri) {
        viewModelScope.launch {
            try {
                val dir = File(context.filesDir, "profile").also { it.mkdirs() }
                val dest = File(dir, "avatar.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(dest).use { output ->
                        input.copyTo(output)
                    }
                }
                appPrefsRepository.setProfilePhotoPath(dest.absolutePath)
                _saveSuccess.emit(Unit)
            } catch (e: Exception) {
                android.util.Log.e("MyProfile", "Fotoğraf kaydedilemedi: ${e.message}")
            }
        }
    }

    /** Profil fotoğrafını kaldır. */
    fun removeProfilePhoto() {
        viewModelScope.launch {
            val path = _uiState.value.profilePhotoPath
            if (path != null) {
                File(path).delete()
                appPrefsRepository.setProfilePhotoPath(null)
            }
        }
    }
}
