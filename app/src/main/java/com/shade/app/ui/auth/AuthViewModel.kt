package com.shade.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shade.app.R
import com.shade.app.crypto.MnemonicManager
import com.shade.app.domain.usecase.LoginUseCase
import com.shade.app.domain.usecase.RegisterUseCase
import com.shade.app.security.KeyVaultManager
import com.shade.app.ui.util.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(
        val message: UiText,
        val mnemonic: List<String> = emptyList(),
        val shadeId: String? = null
    ) : AuthUiState()
    data class Error(val message: UiText) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase,
    private val loginUseCase: LoginUseCase,
    private val keyVaultManager: KeyVaultManager,
    private val mnemonicManager: MnemonicManager
): ViewModel() {
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    fun resetUiState() {
        _uiState.value = AuthUiState.Idle
    }

    fun register(deviceModel: String, fcmToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            val currentMnemonic = mnemonicManager.generateMnemonic()
            val result = registerUseCase(currentMnemonic, deviceModel, fcmToken)

            result.onSuccess { shadeId ->
                keyVaultManager.saveShadeId(shadeId)
                _uiState.value = AuthUiState.Success(
                    message = UiText.StringResource(R.string.account_created),
                    mnemonic = currentMnemonic,
                    shadeId = shadeId
                )
            }.onFailure { error ->
                _uiState.value = AuthUiState.Error(UiText.StringResource(R.string.something_went_wrong))
            }
        }
    }

    fun login(shadeId: String, mnemonic: List<String>, deviceModel: String, fcmToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = loginUseCase(shadeId, mnemonic, deviceModel, fcmToken)

            result.onSuccess { token ->
                keyVaultManager.saveAccessToken(token)
                keyVaultManager.saveShadeId(shadeId)
                _uiState.value = AuthUiState.Success(UiText.StringResource(R.string.login_successful))
            }.onFailure { error ->
                _uiState.value = AuthUiState.Error(UiText.StringResource(R.string.something_went_wrong))
            }
        }
    }
}
