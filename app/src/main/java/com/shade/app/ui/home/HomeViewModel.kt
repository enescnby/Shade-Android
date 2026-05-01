package com.shade.app.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shade.app.data.local.model.ChatWithContact
import com.shade.app.data.remote.websocket.MessageListener
import com.shade.app.domain.repository.ChatRepository
import com.shade.app.security.KeyVaultManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class HomeUiState(
    val chats: List<ChatWithContact> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val messageListener: MessageListener,
    private val keyVaultManager: KeyVaultManager
) : ViewModel() {

    companion object {
        private const val TAG = "SHADE_HOME"
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _loggedOut = MutableStateFlow(false)
    val loggedOut: StateFlow<Boolean> = _loggedOut.asStateFlow()

    init {
        Log.d(TAG, "HomeViewModel başlatıldı")
        messageListener.ensureConnected()
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
