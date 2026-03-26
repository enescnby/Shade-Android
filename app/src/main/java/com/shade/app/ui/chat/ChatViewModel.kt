package com.shade.app.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shade.app.data.local.entities.MessageEntity
import com.shade.app.data.local.entities.MessageStatus
import com.shade.app.data.local.entities.MessageType
import com.shade.app.domain.repository.ChatRepository
import com.shade.app.domain.repository.MessageRepository
import com.shade.app.domain.usecase.message.MarkChatAsReadUseCase
import com.shade.app.domain.usecase.message.SendMessageUseCase
import com.shade.app.security.KeyVaultManager
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
    val firstUnreadMessageId: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val sendMessageUseCase: SendMessageUseCase,
    private val markChatAsReadUseCase: MarkChatAsReadUseCase,
    private val chatRepository: ChatRepository,
    private val keyVaultManager: KeyVaultManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val chatId: String = savedStateHandle["chatId"] ?: ""
    private val initialChatName: String = savedStateHandle["chatName"] ?: ""

    private val _uiState = MutableStateFlow(
        ChatUiState(
            chatId = chatId, 
            chatName = initialChatName,
            myShadeId = keyVaultManager.getShadeId() ?: ""
        )
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var hasCalculatedInitialScroll = false
    init {
        observeMessages()
        observeChatDetails()
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

                if (messages.isNotEmpty() && messages.last().senderId != keyVaultManager.getShadeId()) {
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

    fun clearUnreadNotification() {
        _uiState.update { it.copy(firstUnreadMessageId = null) }
    }
}
