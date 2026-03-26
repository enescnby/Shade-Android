package com.shade.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shade.app.R
import com.shade.app.crypto.MessageCryptoManager
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
    private val messageListener: MessageListener
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private val _lookupState = MutableStateFlow<LookupUiState>(LookupUiState.Idle)
    val lookupState: StateFlow<LookupUiState> = _lookupState.asStateFlow()

    init {
        messageListener.startListening()
        observeChats()
    }

    private fun observeChats() {
        chatRepository.getAllChatsWithContact()
            .onStart { _uiState.update { it.copy(isLoading = true) } }
            .onEach { chatList ->
                _uiState.update { it.copy(chats = chatList, isLoading = false) }
            }
            .catch { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    fun startLookup(shadeId: String, onNavigateToChat: (String, String) -> Unit) {
        viewModelScope.launch {
            _lookupState.value = LookupUiState.Loading

            val contact = contactRepository.getOrFetchContact(shadeId)
            if (contact != null) {
                _lookupState.value = LookupUiState.Success
                onNavigateToChat(contact.shadeId, contact.savedName ?: contact.shadeId)
            } else {
                _lookupState.value = LookupUiState.Error(UiText.StringResource(R.string.user_not_found))
            }
        }
    }

    fun resetLookupState() {
        _lookupState.value = LookupUiState.Idle
    }

    fun deleteChat(chat: ChatWithContact) {
        viewModelScope.launch {
            chatRepository.deleteChat(chat.chat.chatId)
        }
    }
}