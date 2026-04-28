package com.shade.app.ui.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
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
    var messageText by remember { mutableStateOf("") }
    var fullScreenImagePath by remember { mutableStateOf<String?>(null) }

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
                            MessageItem(
                                message = message,
                                isMe = message.senderId == uiState.myShadeId,
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
                                }
                            )
                        }
                    }
                }
            } else {
                val reversedMessages = remember(uiState.messages) { uiState.messages.reversed() }
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    reverseLayout = true
                ) {
                    items(
                        items = reversedMessages,
                        key = { it.messageId }
                    ) { message ->
                        MessageItem(
                            message = message,
                            isMe = message.senderId == uiState.myShadeId,
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
                            }
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
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

                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text("Mesaj yaz...", color = TextMuted)
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
                            maxLines = 4
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        val sendEnabled = messageText.isNotBlank()
                        Surface(
                            onClick = {
                                if (sendEnabled) {
                                    viewModel.sendMessage(messageText)
                                    messageText = ""
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
                                    Icons.AutoMirrored.Filled.Send,
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
    onTranslateRequest: () -> Unit = {}
) {
    val dateFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeString = remember(message.timestamp) {
        dateFormatter.format(Date(message.timestamp))
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

            Surface(
                shape = bubbleShape,
                color = if (isMe) Color.Transparent else BubbleOther,
                border = if (!isMe) androidx.compose.foundation.BorderStroke(
                    0.5.dp, BubbleOtherBorder
                ) else null,
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                val bgModifier = if (isMe) {
                    Modifier.background(
                        Brush.linearGradient(
                            colors = listOf(BubbleMine, BubbleMineEnd)
                        )
                    )
                } else Modifier

                Column(modifier = bgModifier) {
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
                                Text(
                                    text = timeString,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isMe) Color.White.copy(alpha = 0.65f) else TextMuted,
                                    fontSize = 10.sp
                                )
                                if (isMe) {
                                    MessageStatusIcon(status = message.status)
                                }
                            }
                        }
                    }
                }
            }

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
        MessageStatus.PENDING -> Icons.Default.AccessTime
        MessageStatus.SENT -> Icons.Default.Check
        MessageStatus.DELIVERED, MessageStatus.READ -> Icons.Default.DoneAll
        MessageStatus.FAILED -> Icons.Default.ErrorOutline
    }

    val tint = when (status) {
        MessageStatus.READ -> ReadBlue
        MessageStatus.FAILED -> ErrorRed
        else -> if (isImageOverlay) Color.White else Color.White.copy(alpha = 0.6f)
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(14.dp),
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
