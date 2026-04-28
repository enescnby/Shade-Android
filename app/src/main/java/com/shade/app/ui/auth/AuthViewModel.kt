package com.shade.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.shade.app.R
import com.shade.app.crypto.MnemonicManager
import com.shade.app.domain.usecase.auth.LoginUseCase
import com.shade.app.domain.usecase.auth.RegisterUseCase
import com.shade.app.security.KeyVaultManager
import com.shade.app.ui.util.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
    object Authenticated : AuthUiState()
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
    private var fcmToken = ""

    init {
        checkAuthStatus()
        fetchFcmToken()
    }

    private fun checkAuthStatus() {
        viewModelScope.launch {
            if (!keyVaultManager.getAccessToken().isNullOrEmpty()) {
                _uiState.value = AuthUiState.Authenticated
            }
        }
    }

    fun resetUiState() {
        if (_uiState.value is AuthUiState.Authenticated) return
        _uiState.value = AuthUiState.Idle
    }

    private fun fetchFcmToken() {
        viewModelScope.launch {
            try {
                fcmToken = FirebaseMessaging.getInstance().token.await()
            } catch (e: Exception) {
                fcmToken = ""
            }
        }
    }

    fun register(deviceModel: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            val currentMnemonic = mnemonicManager.generateMnemonic()
            val result = registerUseCase(currentMnemonic, deviceModel, fcmToken)

            result.onSuccess { authResult ->
                _uiState.value = AuthUiState.Success(
                    message = UiText.StringResource(R.string.account_created),
                    mnemonic = currentMnemonic,
                    shadeId = authResult.shadeId
                )
            }.onFailure {
                _uiState.value = AuthUiState.Error(UiText.StringResource(R.string.something_went_wrong))
            }
        }
    }

    fun login(shadeId: String, mnemonic: List<String>, deviceModel: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = loginUseCase(shadeId, mnemonic, deviceModel, fcmToken)

            result.onSuccess {
                _uiState.value = AuthUiState.Success(UiText.StringResource(R.string.login_successful))
            }.onFailure {
                _uiState.value = AuthUiState.Error(UiText.StringResource(R.string.something_went_wrong))
            }
        }
    }
}
