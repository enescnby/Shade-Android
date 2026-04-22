package com.shade.app.ui.chat

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shade.app.data.local.entities.MessageEntity
import com.shade.app.data.local.entities.MessageStatus
import com.shade.app.data.local.entities.MessageType
import com.shade.app.domain.repository.ChatRepository
import com.shade.app.domain.repository.MessageRepository
import com.shade.app.domain.usecase.message.DownloadImageUseCase
import com.shade.app.domain.usecase.message.MarkChatAsReadUseCase
import com.shade.app.domain.usecase.message.SendImageMessageUseCase
import com.shade.app.domain.usecase.message.SendMessageUseCase
import com.shade.app.security.KeyVaultManager
import com.shade.app.util.ActiveChatTracker
import com.shade.app.util.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<MessageEntity> = emptyList(),
    val chatName: String = "",
    val chatId: String = "",
    val myShadeId: String = "",
    val initialScrollIndex: Int? = null,
    val firstUnreadMessageId: String? = null,
    val downloadingMessageId: String? = null,
    val downloadProgress: Float = 0f
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val sendMessageUseCase: SendMessageUseCase,
    private val sendImageMessageUseCase: SendImageMessageUseCase,
    private val downloadImageUseCase: DownloadImageUseCase,
    private val markChatAsReadUseCase: MarkChatAsReadUseCase,
    private val chatRepository: ChatRepository,
    private val keyVaultManager: KeyVaultManager,
    private val activeChatTracker: ActiveChatTracker,
    private val notificationHelper: NotificationHelper,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val chatId: String = savedStateHandle["chatId"] ?: ""
    private val initialChatName: String = savedStateHandle["chatName"] ?: ""

    private val _uiState = MutableStateFlow(
        ChatUiState(
            chatId = chatId,
            chatName = initialChatName
        )
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var hasCalculatedInitialScroll = false

    init {
        activeChatTracker.setActive(chatId)
        notificationHelper.clearNotifications(chatId)
        viewModelScope.launch {
            val shadeId = keyVaultManager.getShadeId() ?: ""
            _uiState.update { it.copy(myShadeId = shadeId) }
        }
        observeMessages()
        observeChatDetails()
    }

    override fun onCleared() {
        super.onCleared()
        activeChatTracker.clear()
    }

    private fun observeMessages() {
        messageRepository.getMessagesForChat(chatId)
            .onEach { messages ->
                val myId = _uiState.value.myShadeId
                if (!hasCalculatedInitialScroll && messages.isNotEmpty()) {

                    val firstUnreadIdx = messages.indexOfFirst {
                        it.senderId != myId && it.status != MessageStatus.READ
                    }

                    if (firstUnreadIdx != -1) {
                        val firstUnreadMessageId = messages[firstUnreadIdx].messageId
                        val reversedIndex = (messages.size - 1 - firstUnreadIdx) + 1
                        _uiState.update { it.copy(
                            initialScrollIndex = reversedIndex,
                            firstUnreadMessageId = firstUnreadMessageId
                        )  }
                    }

                    hasCalculatedInitialScroll = true
                }

                _uiState.update { it.copy(messages = messages) }

                val hasUnread = messages.any {
                    it.senderId != myId && it.status != MessageStatus.READ
                }
                if (hasUnread) {
                    markChatAsReadUseCase(chatId)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeChatDetails() {
        chatRepository.observeChatWithContact(chatId)
            .onEach { chatWithContact ->
                chatWithContact?.let { details ->
                    _uiState.update { it.copy(chatName = details.displayName) }
                }
            }
            .launchIn(viewModelScope)
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            sendMessageUseCase(
                receiverShadeId = chatId,
                content = content
            )
        }
    }

    fun sendImage(uri: Uri) {
        viewModelScope.launch {
            sendImageMessageUseCase(
                receiverShadeId = chatId,
                imageUri = uri
            )
        }
    }

    fun downloadImage(message: MessageEntity) {
        viewModelScope.launch {
            _uiState.update { it.copy(downloadingMessageId = message.messageId, downloadProgress = 0f) }
            val result = downloadImageUseCase(message) { progress ->
                _uiState.update { it.copy(downloadProgress = progress) }
            }
            result.onFailure { e ->
                Log.e("ChatViewModel", "Image download failed: ${e.message}", e)
            }
            _uiState.update { it.copy(downloadingMessageId = null, downloadProgress = 0f) }
        }
    }
    fun clearUnreadNotification() {
        _uiState.update { it.copy(firstUnreadMessageId = null) }
    }
}
