package com.shade.app.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shade.app.data.local.entities.MessageType
import com.shade.app.ui.chat.components.*
import com.shade.app.ui.theme.RichBlack
import com.shade.app.ui.theme.TextSecondary
import com.shade.app.worker.AutoDeleteScheduler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onBackClick: () -> Unit,
    onProfileClick: (String) -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Hata mesajlarını Snackbar ile göster
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissError()
        }
    }

    // ── Zamanlayıcı: otomatik silme ─────────────────────────────────────────
    val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    LaunchedEffect(uiState.autoDeleteMinutes, uiState.chatId) {
        if (uiState.chatId.isNotBlank()) {
            AutoDeleteScheduler.schedule(appContext, uiState.chatId, uiState.autoDeleteMinutes)
        }
    }

    // ── UI durumları ─────────────────────────────────────────────────────────
    var messageText by remember { mutableStateOf("") }
    var fullScreenImagePath by remember { mutableStateOf<String?>(null) }
    var showBgPicker by remember { mutableStateOf(false) }
    var showAutoDeletePicker by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var pendingTranslationMessageId by remember { mutableStateOf<String?>(null) }
    var pendingTranslationContent by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    val audioRecorder = remember { AudioRecorderHelper() }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Düzenleme başladığında input'u doldur
    LaunchedEffect(uiState.editingMessage) {
        uiState.editingMessage?.let { messageText = it.content }
    }

    // ── Picker launcher'lar ──────────────────────────────────────────────────
    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.sendImage(it) }
    }
    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.sendFile(it) }
    }

    // ── Mikrofon izni ────────────────────────────────────────────────────────
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            isRecording = true
            audioRecorder.start(context)
        }
    }

    // ── Scroll ───────────────────────────────────────────────────────────────
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (listState.firstVisibleItemIndex <= 1) listState.animateScrollToItem(0)
    }
    LaunchedEffect(uiState.initialScrollIndex) {
        uiState.initialScrollIndex?.let { listState.scrollToItem(it) }
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                if (index == 0 && uiState.firstUnreadMessageId != null) {
                    viewModel.clearUnreadNotification()
                }
            }
    }

    // ── Dialoglar ────────────────────────────────────────────────────────────
    fullScreenImagePath?.let {
        FullScreenImageViewer(imagePath = it, onDismiss = { fullScreenImagePath = null })
    }

    if (showBgPicker) {
        BackgroundPickerDialog(
            currentColor = uiState.chatBackgroundColor,
            onColorSelected = { viewModel.setChatBackground(it) },
            onDismiss = { showBgPicker = false }
        )
    }

    if (showAutoDeletePicker) {
        AutoDeletePickerDialog(
            currentMinutes = uiState.autoDeleteMinutes,
            onMinutesSelected = { min ->
                viewModel.setAutoDeleteMinutes(min)
                AutoDeleteScheduler.schedule(appContext, uiState.chatId, min)
            },
            onDismiss = { showAutoDeletePicker = false }
        )
    }

    if (showLanguageDialog) {
        LanguagePickerDialog(
            onLanguageSelected = { code ->
                pendingTranslationMessageId?.let { id ->
                    viewModel.translateMessage(id, pendingTranslationContent, code)
                }
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    // ── Ana scaffold ─────────────────────────────────────────────────────────
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ChatTopBar(
                chatName = uiState.chatName,
                chatId = uiState.chatId,
                lastSeenText = uiState.lastSeenText,
                autoDeleteMinutes = uiState.autoDeleteMinutes,
                isSearchActive = uiState.isSearchActive,
                searchQuery = uiState.searchQuery,
                onBackClick = onBackClick,
                onProfileClick = onProfileClick,
                onSearchToggle = { viewModel.toggleSearch() },
                onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                onShowBgPicker = { showBgPicker = true },
                onShowAutoDeletePicker = { showAutoDeletePicker = true }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isSearchActive && uiState.searchQuery.isNotBlank()) {
                SearchResults(uiState, onImageClick = { fullScreenImagePath = it }, onTranslateRequest = { id, content ->
                    pendingTranslationMessageId = id; pendingTranslationContent = content; showLanguageDialog = true
                }, viewModel = viewModel)
            } else {
                MessageList(
                    uiState = uiState,
                    listState = listState,
                    onImageClick = { fullScreenImagePath = it },
                    onTranslateRequest = { id, content ->
                        pendingTranslationMessageId = id; pendingTranslationContent = content; showLanguageDialog = true
                    },
                    viewModel = viewModel
                )

                MessageInput(
                    messageText = messageText,
                    replyingTo = uiState.replyingToMessage,
                    editingMessage = uiState.editingMessage,
                    isRecording = isRecording,
                    onMessageTextChange = { messageText = it },
                    onSendOrConfirm = {
                        if (uiState.editingMessage != null) {
                            viewModel.confirmEdit(messageText)
                        } else {
                            viewModel.sendMessage(messageText)
                        }
                        messageText = ""
                    },
                    onCancelReply = { viewModel.cancelReply() },
                    onCancelEditing = { viewModel.cancelEditing(); messageText = "" },
                    onPickImage = {
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onPickFile = { filePickerLauncher.launch("*/*") },
                    onRecordStart = {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPermission) {
                            isRecording = true
                            audioRecorder.start(context)
                        } else {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onRecordCancel = {
                        isRecording = false
                        audioRecorder.cancel()
                    },
                    onRecordStop = {
                        isRecording = false
                        val (file, duration) = audioRecorder.stop()
                        if (file != null && duration > 500) {
                            viewModel.sendAudio(file, duration)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.SearchResults(
    uiState: ChatUiState,
    onImageClick: (String) -> Unit,
    onTranslateRequest: (String, String) -> Unit,
    viewModel: ChatViewModel,
) {
    if (uiState.searchResults.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text("Sonuç bulunamadı", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(uiState.searchResults, key = { it.messageId }) { message ->
                val isMe = message.senderId == uiState.myShadeId
                MessageBubble(
                    message = message, isMe = isMe,
                    isDownloading = uiState.downloadingMessageId == message.messageId,
                    downloadProgress = if (uiState.downloadingMessageId == message.messageId) uiState.downloadProgress else 0f,
                    isDownloadingFile = uiState.downloadingFileMessageId == message.messageId,
                    translatedText = uiState.translatedMessages[message.messageId],
                    isTranslating = uiState.translatingMessageId == message.messageId,
                    onImageClick = onImageClick,
                    onDownloadClick = { viewModel.downloadImage(message) },
                    onDownloadAudioClick = { viewModel.downloadAudio(message) },
                    onDownloadFileClick = { viewModel.downloadFile(message) },
                    onTranslateRequest = { onTranslateRequest(message.messageId, message.content) },
                    onDeleteForMe = { viewModel.deleteForMe(message) },
                    onDeleteForEveryone = if (isMe) {{ viewModel.deleteForEveryone(message) }} else null,
                    onReply = if (!message.isDeleted) {{ viewModel.startReply(message) }} else null,
                    onEdit = if (isMe && message.messageType == MessageType.TEXT && !message.isDeleted) {{ viewModel.startEditing(message) }} else null
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.MessageList(
    uiState: ChatUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onImageClick: (String) -> Unit,
    onTranslateRequest: (String, String) -> Unit,
    viewModel: ChatViewModel,
) {
    val pagedMessages = viewModel.pagedMessages.collectAsLazyPagingItems()
    val chatBgColor = uiState.chatBackgroundColor?.let { Color(it) }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .background(chatBgColor ?: RichBlack),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        reverseLayout = true   // PagingSource DESC sıralı; reverseLayout ile en yeni altta görünür
    ) {
        items(
            count = pagedMessages.itemCount,
            key = pagedMessages.itemKey { it.messageId }
        ) { index ->
            val message = pagedMessages[index] ?: return@items
            val isMe = message.senderId == uiState.myShadeId
            MessageBubble(
                message = message, isMe = isMe,
                isDownloading = uiState.downloadingMessageId == message.messageId,
                downloadProgress = if (uiState.downloadingMessageId == message.messageId) uiState.downloadProgress else 0f,
                isDownloadingFile = uiState.downloadingFileMessageId == message.messageId,
                translatedText = uiState.translatedMessages[message.messageId],
                isTranslating = uiState.translatingMessageId == message.messageId,
                onImageClick = onImageClick,
                onDownloadClick = { viewModel.downloadImage(message) },
                onDownloadAudioClick = { viewModel.downloadAudio(message) },
                onDownloadFileClick = { viewModel.downloadFile(message) },
                onTranslateRequest = { onTranslateRequest(message.messageId, message.content) },
                onDeleteForMe = { viewModel.deleteForMe(message) },
                onDeleteForEveryone = if (isMe) {{ viewModel.deleteForEveryone(message) }} else null,
                onEdit = if (isMe && message.messageType == MessageType.TEXT && !message.isDeleted) {{ viewModel.startEditing(message) }} else null,
                onReply = if (!message.isDeleted) {{ viewModel.startReply(message) }} else null
            )

            if (message.messageId == uiState.firstUnreadMessageId) {
                UnreadMessagesHeader()
            }
        }
    }
}
