package com.shade.app.ui.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Close
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.shade.app.R
import com.shade.app.data.local.entities.MessageEntity
import com.shade.app.data.local.entities.MessageStatus
import com.shade.app.data.local.entities.MessageType
import com.shade.app.ui.theme.*
import com.shade.app.ui.util.UiText
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private val LANGUAGES = listOf(
    "🇬🇧 İngilizce" to "en",
    "🇩🇪 Almanca" to "de",
    "🇫🇷 Fransızca" to "fr",
    "🇪🇸 İspanyolca" to "es",
    "🇸🇦 Arapça" to "ar",
    "🇷🇺 Rusça" to "ru",
    "🇨🇳 Çince" to "zh",
    "🇯🇵 Japonca" to "ja",
    "🇮🇹 İtalyanca" to "it",
    "🇧🇷 Portekizce" to "pt",
    "🇹🇷 Türkçe" to "tr"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onBackClick: () -> Unit,
    onProfileClick: (String) -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var messageText by remember { mutableStateOf("") }
    var fullScreenImagePath by remember { mutableStateOf<String?>(null) }
    var showChatOptions by remember { mutableStateOf(false) }
    var showBgPicker by remember { mutableStateOf(false) }
    var showAutoDeletePicker by remember { mutableStateOf(false) }

    // Editing mode: prefill input when edit starts
    LaunchedEffect(uiState.editingMessage) {
        uiState.editingMessage?.let { messageText = it.content }
    }

    // Chat açılınca otomatik silme worker'ını başlat (ayar varsa)
    LaunchedEffect(uiState.autoDeleteMinutes, uiState.chatId) {
        if (uiState.chatId.isNotBlank()) {
            com.shade.app.worker.AutoDeleteWorker.schedule(context, uiState.chatId, uiState.autoDeleteMinutes)
        }
    }

    // Background color picker dialog
    if (showBgPicker) {
        val bgColors = listOf(
            null to "Varsayılan",
            0xFF1A1A2E.toInt() to "Gece Mavisi",
            0xFF0D1B2A.toInt() to "Derin Lacivert",
            0xFF1B1B1B.toInt() to "Siyah",
            0xFF1A2A1A.toInt() to "Orman Yeşili",
            0xFF2A1A1A.toInt() to "Bordo",
            0xFF1A1A3A.toInt() to "Mor Gece",
            0xFF2A2A1A.toInt() to "Çöl Altını",
            0xFF0A0A0A.toInt() to "Jet Siyahı"
        )
        AlertDialog(
            onDismissRequest = { showBgPicker = false },
            containerColor = SurfaceDark,
            title = { Text("Arkaplan Rengi", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    bgColors.forEach { (argb, label) ->
                        TextButton(
                            onClick = {
                                viewModel.setChatBackground(argb)
                                showBgPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            if (argb == null) RichBlack else Color(argb),
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    label,
                                    color = if (uiState.chatBackgroundColor == argb) AccentPurple else TextPrimary
                                )
                                if (uiState.chatBackgroundColor == argb) {
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(Icons.Default.Check, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBgPicker = false }) { Text("İptal", color = TextMuted) }
            }
        )
    }

    // Auto-delete picker dialog
    if (showAutoDeletePicker) {
        val options = listOf(0 to "Kapalı", 1 to "1 dakika", 5 to "5 dakika", 60 to "1 saat", 1440 to "24 saat", 10080 to "7 gün")
        AlertDialog(
            onDismissRequest = { showAutoDeletePicker = false },
            containerColor = SurfaceDark,
            title = { Text("Otomatik Silme", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    options.forEach { (min, label) ->
                        TextButton(
                            onClick = {
                                viewModel.setAutoDeleteMinutes(min)
                                com.shade.app.worker.AutoDeleteWorker.schedule(context, uiState.chatId, min)
                                showAutoDeletePicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    label,
                                    color = if (uiState.autoDeleteMinutes == min) AccentPurple else TextPrimary
                                )
                                if (uiState.autoDeleteMinutes == min) {
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(Icons.Default.Check, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAutoDeletePicker = false }) { Text("İptal", color = TextMuted) }
            }
        )
    }

    // Translation dialog state
    var pendingTranslationMessageId by remember { mutableStateOf<String?>(null) }
    var pendingTranslationContent by remember { mutableStateOf("") }
    var showLanguageDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.sendImage(it) }
    }

    // Language selection dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Dil Seçin", color = TextPrimary) },
            text = {
                LazyColumn {
                    items(LANGUAGES) { (label, code) ->
                        TextButton(
                            onClick = {
                                showLanguageDialog = false
                                pendingTranslationMessageId?.let { id ->
                                    viewModel.translateMessage(id, pendingTranslationContent, code)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(label, modifier = Modifier.fillMaxWidth(), fontSize = 15.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text("İptal") }
            }
        )
    }

    LaunchedEffect(uiState.messages.size) {
        if (listState.firstVisibleItemIndex <= 1) {
            listState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(uiState.initialScrollIndex) {
        uiState.initialScrollIndex?.let { index ->
            listState.scrollToItem(index)
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                if (index == 0 && uiState.firstUnreadMessageId != null) {
                    viewModel.clearUnreadNotification()
                }
            }
    }

    fullScreenImagePath?.let { path ->
        FullScreenImageViewer(
            imagePath = path,
            onDismiss = { fullScreenImagePath = null }
        )
    }

    Scaffold(
        containerColor = RichBlack,
        topBar = {
            if (uiState.isSearchActive) {
                Surface(
                    color = SurfaceDark,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.toggleSearch() }) {
                            Icon(Icons.Default.Close, contentDescription = "Aramayı Kapat", tint = TextPrimary)
                        }
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChange(it) },
                            placeholder = { Text("Mesajlarda ara...", color = TextMuted) },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentPurple,
                                unfocusedBorderColor = OutlineMuted,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = AccentPurple
                            )
                        )
                    }
                }
            } else {
                Surface(
                    color = SurfaceDark,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Geri",
                                tint = TextPrimary
                            )
                        }

                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = BubbleMine
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = uiState.chatName.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onProfileClick(uiState.chatId) }
                        ) {
                            Text(
                                text = uiState.chatName,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            val subtitle = uiState.lastSeenText.ifBlank { "Profil detayları" }
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (uiState.lastSeenText == "Çevrimiçi")
                                    Color(0xFF4CAF50)
                                else
                                    TextMuted
                            )
                        }

                        IconButton(onClick = { viewModel.toggleSearch() }) {
                            Icon(Icons.Default.Search, contentDescription = "Mesajlarda Ara", tint = TextPrimary)
                        }
                        Box {
                            IconButton(onClick = { showChatOptions = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Seçenekler", tint = TextPrimary)
                            }
                            DropdownMenu(
                                expanded = showChatOptions,
                                onDismissRequest = { showChatOptions = false },
                                modifier = Modifier.background(SurfaceDark)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Arkaplan Rengi", color = TextPrimary) },
                                    leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null, tint = AccentPurple) },
                                    onClick = { showChatOptions = false; showBgPicker = true }
                                )
                                DropdownMenuItem(
                                    text = {
                                        val label = if (uiState.autoDeleteMinutes == 0) "Otomatik Silme: Kapalı"
                                        else "Otomatik Silme: Açık"
                                        Text(label, color = TextPrimary)
                                    },
                                    leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null, tint = AccentPurple) },
                                    onClick = { showChatOptions = false; showAutoDeletePicker = true }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isSearchActive && uiState.searchQuery.isNotBlank()) {
                if (uiState.searchResults.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Sonuç bulunamadı",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(uiState.searchResults, key = { it.messageId }) { message ->
                            val isMe = message.senderId == uiState.myShadeId
                            MessageItem(
                                message = message,
                                isMe = isMe,
                                isDownloading = uiState.downloadingMessageId == message.messageId,
                                downloadProgress = if (uiState.downloadingMessageId == message.messageId) uiState.downloadProgress else 0f,
                                translatedText = uiState.translatedMessages[message.messageId],
                                isTranslating = uiState.translatingMessageId == message.messageId,
                                onImageClick = { path -> fullScreenImagePath = path },
                                onDownloadClick = { viewModel.downloadImage(message) },
                                onTranslateRequest = {
                                    pendingTranslationMessageId = message.messageId
                                    pendingTranslationContent = message.content
                                    showLanguageDialog = true
                                },
                                onDeleteForMe = { viewModel.deleteForMe(message) },
                                onDeleteForEveryone = if (isMe) {{ viewModel.deleteForEveryone(message) }} else null,
                                onReply = if (!message.isDeleted) {{ viewModel.startReply(message) }} else null,
                                onEdit = if (isMe && message.messageType == MessageType.TEXT && !message.isDeleted) {
                                    { viewModel.startEditing(message) }
                                } else null
                            )
                        }
                    }
                }
            } else {
                val reversedMessages = remember(uiState.messages) { uiState.messages.reversed() }
                val chatBgColor = uiState.chatBackgroundColor?.let { Color(it) }
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(chatBgColor ?: RichBlack),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    reverseLayout = true
                ) {
                    items(
                        items = reversedMessages,
                        key = { it.messageId }
                    ) { message ->
                        val isMe = message.senderId == uiState.myShadeId
                        MessageItem(
                            message = message,
                            isMe = isMe,
                            isDownloading = uiState.downloadingMessageId == message.messageId,
                            downloadProgress = if (uiState.downloadingMessageId == message.messageId) uiState.downloadProgress else 0f,
                            translatedText = uiState.translatedMessages[message.messageId],
                            isTranslating = uiState.translatingMessageId == message.messageId,
                            onImageClick = { path -> fullScreenImagePath = path },
                            onDownloadClick = { viewModel.downloadImage(message) },
                            onTranslateRequest = {
                                pendingTranslationMessageId = message.messageId
                                pendingTranslationContent = message.content
                                showLanguageDialog = true
                            },
                            onDeleteForMe = { viewModel.deleteForMe(message) },
                            onDeleteForEveryone = if (isMe) {{ viewModel.deleteForEveryone(message) }} else null,
                            onEdit = if (isMe && message.messageType == MessageType.TEXT && !message.isDeleted) {
                                { viewModel.startEditing(message) }
                            } else null,
                            onReply = if (!message.isDeleted) {{ viewModel.startReply(message) }} else null
                        )

                        if (message.messageId == uiState.firstUnreadMessageId) {
                            UnreadMessagesHeader()
                        }
                    }
                }

                // Modern input bar
                Surface(
                    color = SurfaceDark,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        // Reply mode indicator
                        val replyingTo = uiState.replyingToMessage
                        if (replyingTo != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AccentPurple.copy(alpha = 0.10f))
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Reply,
                                    contentDescription = null,
                                    tint = AccentPurple,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Yanıtlanıyor",
                                        color = AccentPurple,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        replyingTo.content.take(60),
                                        color = TextMuted,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.cancelReply() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "İptal",
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Snapshot delegated properties → local vals for smart cast
                        val editingMsg = uiState.editingMessage

                        // Edit mode indicator
                        if (editingMsg != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AccentPurple.copy(alpha = 0.12f))
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = AccentPurple,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Mesajı düzenle",
                                    color = AccentPurple,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        viewModel.cancelEditing()
                                        messageText = ""
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "İptal",
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            if (editingMsg == null) {
                                IconButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Image,
                                        contentDescription = "Fotoğraf",
                                        tint = AccentPurple,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = messageText,
                                onValueChange = { messageText = it },
                                modifier = Modifier.weight(1f),
                                placeholder = {
                                    Text(
                                        if (editingMsg != null) "Düzenle..." else "Mesaj yaz...",
                                        color = TextMuted
                                    )
                                },
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = SurfaceContainer,
                                    unfocusedContainerColor = SurfaceContainer,
                                    focusedBorderColor = AccentPurple.copy(alpha = 0.5f),
                                    unfocusedBorderColor = Color.Transparent,
                                    cursorColor = AccentPurple,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                maxLines = 4,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        if (messageText.isNotBlank()) {
                                            if (editingMsg != null) {
                                                viewModel.confirmEdit(messageText)
                                            } else {
                                                viewModel.sendMessage(messageText)
                                            }
                                            messageText = ""
                                        }
                                    }
                                )
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            val sendEnabled = messageText.isNotBlank()
                            Surface(
                                onClick = {
                                    if (sendEnabled) {
                                        if (editingMsg != null) {
                                            viewModel.confirmEdit(messageText)
                                            messageText = ""
                                        } else {
                                            viewModel.sendMessage(messageText)
                                            messageText = ""
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .align(Alignment.Bottom),
                                shape = CircleShape,
                                color = if (sendEnabled) AccentPurple else SurfaceContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        if (editingMsg != null) Icons.Default.Check
                                        else Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Gönder",
                                        tint = if (sendEnabled) Color.White else TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItem(
    message: MessageEntity,
    isMe: Boolean,
    isDownloading: Boolean = false,
    downloadProgress: Float = 0f,
    translatedText: String? = null,
    isTranslating: Boolean = false,
    onImageClick: (String) -> Unit = {},
    onDownloadClick: () -> Unit = {},
    onTranslateRequest: () -> Unit = {},
    onDeleteForMe: () -> Unit = {},
    onDeleteForEveryone: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onReply: (() -> Unit)? = null
) {
    val dateFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeString = remember(message.timestamp) {
        dateFormatter.format(Date(message.timestamp))
    }

    var showMenu by remember { mutableStateOf(false) }
    var showDeleteForMeDialog by remember { mutableStateOf(false) }
    var showDeleteForEveryoneDialog by remember { mutableStateOf(false) }

    // Kendimden sil onayı
    if (showDeleteForMeDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteForMeDialog = false },
            containerColor = SurfaceDark,
            title = { Text("Mesajı Sil", color = TextPrimary) },
            text = { Text("Bu mesaj yalnızca senin için silinecek.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { showDeleteForMeDialog = false; onDeleteForMe() }) {
                    Text("Sil", color = Color(0xFFFF5252))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteForMeDialog = false }) {
                    Text("İptal", color = TextMuted)
                }
            }
        )
    }

    // Herkesten sil onayı
    if (showDeleteForEveryoneDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteForEveryoneDialog = false },
            containerColor = SurfaceDark,
            title = { Text("Herkesten Sil", color = TextPrimary) },
            text = { Text("Bu mesaj her iki taraf için de silinecek. Bu işlem geri alınamaz.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { showDeleteForEveryoneDialog = false; onDeleteForEveryone?.invoke() }) {
                    Text("Herkesten Sil", color = Color(0xFFFF5252))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteForEveryoneDialog = false }) {
                    Text("İptal", color = TextMuted)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isMe) 48.dp else 0.dp,
                end = if (isMe) 0.dp else 48.dp,
                top = 2.dp,
                bottom = 2.dp
            ),
        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
            val bubbleShape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isMe) 18.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 18.dp
            )

            // Mesaj balonu + bağlam menüsü — aynı Box'ta olunca menü doğru konumlanır
            Box {
                // Uzun basma menüsü (balona göre konumlanır)
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(SurfaceDark)
                ) {
                    if (onReply != null) {
                        DropdownMenuItem(
                            text = { Text("Yanıtla", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Reply, contentDescription = null, tint = AccentPurple) },
                            onClick = { showMenu = false; onReply() }
                        )
                    }
                    if (onEdit != null) {
                        DropdownMenuItem(
                            text = { Text("Düzenle", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = AccentPurple) },
                            onClick = { showMenu = false; onEdit() }
                        )
                    }
                    if (onDeleteForEveryone != null) {
                        DropdownMenuItem(
                            text = { Text("Herkesten Sil", color = Color(0xFFFF5252)) },
                            leadingIcon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFFF5252)) },
                            onClick = { showMenu = false; showDeleteForEveryoneDialog = true }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Kendimden Sil", color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = TextSecondary) },
                        onClick = { showMenu = false; showDeleteForMeDialog = true }
                    )
                }

                Surface(
                    shape = bubbleShape,
                    color = if (isMe) Color.Transparent else BubbleOther,
                    border = if (!isMe) androidx.compose.foundation.BorderStroke(
                        0.5.dp, BubbleOtherBorder
                    ) else null,
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,  // Kendi arkaplan gradyanı var, ripple istemiyoruz
                            onClick = {},
                            onLongClick = { showMenu = true }
                        )
                ) {
                val bgModifier = if (isMe) {
                    Modifier.background(
                        Brush.linearGradient(
                            colors = listOf(BubbleMine, BubbleMineEnd)
                        )
                    )
                } else Modifier

                if (message.isDeleted) {
                    // Silindi durumu
                    Row(
                        modifier = bgModifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Block,
                            contentDescription = null,
                            tint = if (isMe) Color.White.copy(alpha = 0.5f) else TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            "Bu mesaj silindi",
                            color = if (isMe) Color.White.copy(alpha = 0.5f) else TextMuted,
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic
                        )
                    }
                } else Column(modifier = bgModifier) {
                    if (message.messageType == MessageType.IMAGE) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            if (message.imagePath != null) {
                                AsyncImage(
                                    model = File(message.imagePath),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(bubbleShape)
                                        .clickable { onImageClick(message.imagePath) },
                                    contentScale = ContentScale.FillWidth
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    if (message.thumbnailPath != null) {
                                        AsyncImage(
                                            model = File(message.thumbnailPath),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(bubbleShape),
                                            contentScale = ContentScale.FillWidth,
                                            alpha = 0.5f
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(160.dp)
                                                .background(SurfaceContainer)
                                        )
                                    }

                                    if (isDownloading) {
                                        val animatedProgress by animateFloatAsState(
                                            targetValue = downloadProgress,
                                            animationSpec = tween(300),
                                            label = "progress"
                                        )
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.Black.copy(alpha = 0.6f),
                                            modifier = Modifier.size(64.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(
                                                    progress = { animatedProgress },
                                                    modifier = Modifier.size(56.dp),
                                                    color = AccentPurple,
                                                    trackColor = Color.White.copy(alpha = 0.15f),
                                                    strokeWidth = 3.dp
                                                )
                                                Text(
                                                    text = "${(downloadProgress * 100).toInt()}%",
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    } else {
                                        Surface(
                                            onClick = onDownloadClick,
                                            shape = CircleShape,
                                            color = Color.Black.copy(alpha = 0.55f),
                                            modifier = Modifier.size(56.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.ArrowDownward,
                                                    contentDescription = "Görseli indir",
                                                    modifier = Modifier.size(26.dp),
                                                    tint = Color.White
                                                )
                                            }
                                        }
                                    }
                                }

                                // Overlay timestamp for images
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .background(
                                            Color.Black.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = timeString,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontSize = 10.sp
                                    )
                                    if (isMe) {
                                        MessageStatusIcon(status = message.status, isImageOverlay = true)
                                    }
                                }
                            }
                        }
                    }

                    if (message.messageType == MessageType.TEXT) {
                        Column {
                            // Reply preview (quoted message)
                            if (!message.replyToContent.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                                    color = if (isMe) Color.White.copy(alpha = 0.12f)
                                            else SurfaceContainer.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 8.dp, end = 8.dp, top = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(32.dp)
                                                .background(
                                                    if (isMe) Color.White.copy(alpha = 0.8f) else AccentPurple,
                                                    RoundedCornerShape(2.dp)
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = message.replyToContent,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isMe) Color.White.copy(alpha = 0.7f) else TextMuted,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Text(
                                text = message.content,
                                color = if (isMe) Color.White else TextPrimary,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(
                                    start = 12.dp, end = 12.dp,
                                    top = 8.dp, bottom = 2.dp
                                )
                            )

                            // Translation loading
                            if (isTranslating) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .align(Alignment.CenterHorizontally)
                                        .padding(bottom = 4.dp),
                                    strokeWidth = 2.dp,
                                    color = if (isMe) Color.White else AccentPurple
                                )
                            }

                            // Translated text
                            if (!translatedText.isNullOrBlank() && !isTranslating) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    color = if (isMe) Color.White.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.3f)
                                )
                                Text(
                                    text = translatedText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isMe) Color.White.copy(alpha = 0.85f) else TextSecondary,
                                    fontStyle = FontStyle.Italic,
                                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 4.dp)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .padding(end = 10.dp, bottom = 6.dp, start = 10.dp)
                            ) {
                                if (message.isEdited) {
                                    Text(
                                        "düzenlendi",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isMe) Color.White.copy(alpha = 0.45f) else TextMuted,
                                        fontSize = 9.sp,
                                        fontStyle = FontStyle.Italic
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                }
                                Text(
                                    text = timeString,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isMe) Color.White.copy(alpha = 0.65f) else TextMuted,
                                    fontSize = 10.sp
                                )
                                if (isMe) {
                                    MessageStatusIcon(status = message.status)
                                }
                            } // Row (saat+tik) kapandı
                        } // TEXT Column kapandı
                    } // TEXT if kapandı
                } // else Column(bgModifier) kapandı
                } // Surface kapandı
            } // Box kapandı

            // Translate button (only for text messages)
            if (message.messageType == MessageType.TEXT) {
                IconButton(
                    onClick = onTranslateRequest,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Çevir",
                        modifier = Modifier.size(13.dp),
                        tint = TextMuted.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun MessageStatusIcon(status: MessageStatus, isImageOverlay: Boolean = false) {
    val icon = when (status) {
        MessageStatus.PENDING   -> Icons.Default.AccessTime
        MessageStatus.SENT      -> Icons.Default.Check       // ✓  tek tik
        MessageStatus.DELIVERED -> Icons.Default.DoneAll     // ✓✓ çift tik (gri)
        MessageStatus.READ      -> Icons.Default.DoneAll     // ✓✓ çift tik (mavi)
        MessageStatus.FAILED    -> Icons.Default.ErrorOutline
    }

    val tint = if (isImageOverlay) {
        // Fotoğraf üzerinde her zaman beyaz, sadece READ mavi
        when (status) {
            MessageStatus.READ   -> ReadBlue
            MessageStatus.FAILED -> ErrorRed
            else                 -> Color.White
        }
    } else {
        when (status) {
            MessageStatus.PENDING   -> Color.White.copy(alpha = 0.35f) // saat — soluk
            MessageStatus.SENT      -> Color.White.copy(alpha = 0.80f) // tek beyaz tik
            MessageStatus.DELIVERED -> Color(0xFFB0BEC5)               // çift GRİ tik
            MessageStatus.READ      -> ReadBlue                        // çift MAVİ tik
            MessageStatus.FAILED    -> ErrorRed
        }
    }

    Icon(
        imageVector = icon,
        contentDescription = when (status) {
            MessageStatus.PENDING   -> "Gönderiliyor"
            MessageStatus.SENT      -> "Gönderildi"
            MessageStatus.DELIVERED -> "İletildi"
            MessageStatus.READ      -> "Okundu"
            MessageStatus.FAILED    -> "Hata"
        },
        modifier = Modifier.size(16.dp),
        tint = tint
    )
}

@Composable
fun UnreadMessagesHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = AccentPurple.copy(alpha = 0.15f),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(
                0.5.dp, AccentPurple.copy(alpha = 0.3f)
            )
        ) {
            Text(
                text = UiText.StringResource(R.string.unread_messages).asString(),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = AccentPurple,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun FullScreenImageViewer(
    imagePath: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .systemBarsPadding()
        ) {
            ZoomableImage(
                model = File(imagePath),
                modifier = Modifier.fillMaxSize(),
                onTap = onDismiss
            )

            Surface(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(40.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Kapat",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ZoomableImage(
    model: Any?,
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {}
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                    if (newScale == 1f) {
                        offset = androidx.compose.ui.geometry.Offset.Zero
                    } else {
                        offset += pan
                    }
                    scale = newScale
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = androidx.compose.ui.geometry.Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    },
                    onTap = { onTap() }
                )
            }
    ) {
        val animatedScale by animateFloatAsState(
            targetValue = scale,
            animationSpec = tween(200),
            label = "zoom"
        )

        AsyncImage(
            model = model,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = animatedScale,
                    scaleY = animatedScale,
                    translationX = offset.x,
                    translationY = offset.y
                ),
            contentScale = ContentScale.Fit
        )
    }
}
