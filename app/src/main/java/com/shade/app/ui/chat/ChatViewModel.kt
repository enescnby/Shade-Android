package com.shade.app.ui.chat

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shade.app.data.local.entities.MessageEntity
import com.shade.app.data.local.entities.MessageStatus
import com.shade.app.data.remote.api.UserService
import com.shade.app.data.repository.TranslationRepository
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class ChatUiState(
    val messages: List<MessageEntity> = emptyList(),
    val chatName: String = "",
    val chatId: String = "",
    val myShadeId: String = "",
    val initialScrollIndex: Int? = null,
    val firstUnreadMessageId: String? = null,
    val isSendingImage: Boolean = false,
    val downloadingMessageId: String? = null,
    val downloadProgress: Float = 0f,
    // Search
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<MessageEntity> = emptyList(),
    // Last seen
    val lastSeenText: String = "",
    // Translation
    val translatedMessages: Map<String, String> = emptyMap(),
    val translatingMessageId: String? = null
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
    private val userService: UserService,
    private val translationRepository: TranslationRepository,
    private val activeChatTracker: ActiveChatTracker,
    private val notificationHelper: NotificationHelper,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val TAG = "SHADE_CHAT"
    }

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
        Log.d(TAG, "ChatViewModel başlatıldı: chatId=$chatId")
        activeChatTracker.setActive(chatId)
        notificationHelper.clearNotifications(chatId)
        viewModelScope.launch {
            val shadeId = keyVaultManager.getShadeId() ?: ""
            _uiState.update { it.copy(myShadeId = shadeId) }
        }
        observeMessages()
        observeChatDetails()
        fetchUserStatus()
        viewModelScope.launch { markChatAsReadUseCase(chatId) }
    }

    override fun onCleared() {
        super.onCleared()
        activeChatTracker.clear()
        Log.d(TAG, "ChatViewModel temizlendi: chatId=$chatId")
    }

    private fun observeMessages() {
        messageRepository.getMessagesForChat(chatId)
            .onEach { messages ->
                Log.d(TAG, "Mesaj listesi güncellendi: ${messages.size} mesaj")
                val myId = _uiState.value.myShadeId

                if (!hasCalculatedInitialScroll && messages.isNotEmpty()) {
                    val firstUnreadIdx = messages.indexOfFirst {
                        it.senderId != myId && it.status != MessageStatus.READ
                    }
                    if (firstUnreadIdx != -1) {
                        val firstUnreadMessageId = messages[firstUnreadIdx].messageId
                        val reversedIndex = (messages.size - 1 - firstUnreadIdx) + 1
                        _uiState.update {
                            it.copy(
                                initialScrollIndex = reversedIndex,
                                firstUnreadMessageId = firstUnreadMessageId
                            )
                        }
                    }
                    hasCalculatedInitialScroll = true
                }

                _uiState.update { it.copy(messages = messages) }

                val hasUnread = messages.any {
                    it.senderId != myId && it.status != MessageStatus.READ
                }
                if (hasUnread) {
                    viewModelScope.launch { markChatAsReadUseCase(chatId) }
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

    private fun fetchUserStatus() {
        viewModelScope.launch {
            try {
                val token = "Bearer ${keyVaultManager.getAccessToken()}"
                val response = userService.getUserStatus(token, chatId)
                if (response.isSuccessful) {
                    val status = response.body() ?: return@launch
                    val text = when {
                        status.isOnline -> "Çevrimiçi"
                        status.lastActive.isNullOrBlank() -> ""
                        else -> {
                            val instant = Instant.parse(status.lastActive)
                            val minutesAgo = ChronoUnit.MINUTES.between(instant, Instant.now())
                            when {
                                minutesAgo < 60 -> "Son görülme: $minutesAgo dakika önce"
                                minutesAgo < 1440 -> "Son görülme: ${minutesAgo / 60} saat önce"
                                else -> {
                                    val formatter = DateTimeFormatter.ofPattern("d MMM")
                                        .withZone(ZoneId.systemDefault())
                                    "Son görülme: ${formatter.format(instant)}"
                                }
                            }
                        }
                    }
                    _uiState.update { it.copy(lastSeenText = text) }
                    Log.d(TAG, "Son görülme: $text")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Son görülme alınamadı: ${e.message}")
            }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return
        Log.d(TAG, "Metin mesajı gönderiliyor → chatId=$chatId")
        viewModelScope.launch {
            sendMessageUseCase(receiverShadeId = chatId, content = content)
            Log.d(TAG, "Metin mesajı gönderildi")
        }
    }

    fun sendImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSendingImage = true) }
            sendImageMessageUseCase(receiverShadeId = chatId, imageUri = uri)
            _uiState.update { it.copy(isSendingImage = false) }
        }
    }

    fun downloadImage(message: MessageEntity) {
        viewModelScope.launch {
            _uiState.update { it.copy(downloadingMessageId = message.messageId, downloadProgress = 0f) }
            val result = downloadImageUseCase(message) { progress ->
                _uiState.update { it.copy(downloadProgress = progress) }
            }
            result.onFailure { e ->
                Log.e(TAG, "Image download failed: ${e.message}", e)
            }
            _uiState.update { it.copy(downloadingMessageId = null, downloadProgress = 0f) }
        }
    }

    fun clearUnreadNotification() {
        _uiState.update { it.copy(firstUnreadMessageId = null) }
    }

    fun toggleSearch() {
        val nowActive = !_uiState.value.isSearchActive
        _uiState.update { it.copy(isSearchActive = nowActive, searchQuery = "", searchResults = emptyList()) }
        Log.d(TAG, "Arama modu: $nowActive")
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList()) }
            return
        }
        messageRepository.searchMessages(chatId, query)
            .onEach { results ->
                Log.d(TAG, "Arama sonucu: ${results.size} mesaj ('$query')")
                _uiState.update { it.copy(searchResults = results) }
            }
            .catch { e -> Log.e(TAG, "Arama hatası: ${e.message}") }
            .launchIn(viewModelScope)
    }

    fun translateMessage(messageId: String, content: String, targetLang: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(translatingMessageId = messageId) }
            val translated = translationRepository.translate(content, targetLang)
            if (!translated.isNullOrBlank()) {
                _uiState.update { state ->
                    state.copy(
                        translatedMessages = state.translatedMessages + (messageId to translated),
                        translatingMessageId = null
                    )
                }
                Log.d(TAG, "Çeviri tamamlandı: $translated")
            } else {
                Log.w(TAG, "Çeviri sonuç boş")
                _uiState.update { it.copy(translatingMessageId = null) }
            }
        }
    }
}
