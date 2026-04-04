package com.shade.app.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shade.app.R
import com.shade.app.data.local.entities.ChatEntity
import com.shade.app.data.local.entities.ContactEntity
import com.shade.app.data.local.entities.MessageEntity
import com.shade.app.data.local.entities.MessageStatus
import com.shade.app.data.local.entities.MessageType
import com.shade.app.data.local.model.ChatWithContact
import com.shade.app.data.remote.api.UserService
import com.shade.app.data.remote.websocket.MessageListener
import com.shade.app.data.remote.websocket.ShadeWebSocketManager
import com.shade.app.domain.repository.ChatRepository
import com.shade.app.domain.repository.ContactRepository
import com.shade.app.domain.repository.MessageRepository
import com.shade.app.security.KeyVaultManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.shade.app.ui.util.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.bouncycastle.util.encoders.Hex
import javax.inject.Inject

data class HomeUiState(
    val chats: List<ChatWithContact> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class LookupUiState {
    object Idle : LookupUiState()
    object Loading : LookupUiState()
    object Success : LookupUiState()
    data class Error(val message: UiText) : LookupUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val contactRepository: ContactRepository,
    private val messageListener: MessageListener,
    private val keyVaultManager: KeyVaultManager
) : ViewModel() {

    companion object {
        private const val TAG = "SHADE_HOME"
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private val _lookupState = MutableStateFlow<LookupUiState>(LookupUiState.Idle)
    val lookupState: StateFlow<LookupUiState> = _lookupState.asStateFlow()
    private val _loggedOut = MutableStateFlow(false)
    val loggedOut: StateFlow<Boolean> = _loggedOut.asStateFlow()

    init {
        Log.d(TAG, "HomeViewModel başlatıldı")
        messageListener.startListening()
        observeChats()
    }

    private fun observeChats() {
        Log.d(TAG, "Sohbetler dinleniyor...")
        chatRepository.getAllChatsWithContact()
            .onStart { _uiState.update { it.copy(isLoading = true) } }
            .onEach { chatList ->
                Log.d(TAG, "Sohbet listesi güncellendi: ${chatList.size} sohbet")
                _uiState.update { it.copy(chats = chatList, isLoading = false) }
            }
            .catch { e ->
                Log.e(TAG, "Sohbet listesi alınamadı: ${e.message}")
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    fun startLookup(shadeId: String, onNavigateToChat: (String, String) -> Unit) {
        Log.d(TAG, "Kullanıcı aranıyor: $shadeId")
        viewModelScope.launch {
            _lookupState.value = LookupUiState.Loading

            val contact = contactRepository.getOrFetchContact(shadeId)
            if (contact != null) {
                Log.d(TAG, "Kullanıcı bulundu: $shadeId → chat başlatılıyor")
                _lookupState.value = LookupUiState.Success
                onNavigateToChat(contact.shadeId, contact.savedName ?: contact.shadeId)
            } else {
                Log.w(TAG, "Kullanıcı bulunamadı: $shadeId")
                _lookupState.value = LookupUiState.Error(UiText.StringResource(R.string.user_not_found))
            }
        }
    }

    fun resetLookupState() {
        Log.d(TAG, "Lookup state sıfırlandı")
        _lookupState.value = LookupUiState.Idle
    }

    fun deleteChat(chat: ChatWithContact) {
        Log.d(TAG, "Sohbet siliniyor: ${chat.chat.chatId}")
        viewModelScope.launch {
            chatRepository.deleteChat(chat.chat.chatId)
            Log.d(TAG, "Sohbet silindi: ${chat.chat.chatId}")
        }
    }

    fun logout() {
        Log.d(TAG, "Çıkış yapılıyor...")
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                keyVaultManager.clearVault()
            }
            _loggedOut.value = true
            Log.d(TAG, "Çıkış tamamlandı")
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "HomeViewModel temizlendi")
    }
}