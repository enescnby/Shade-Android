package com.shade.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shade.app.R
import com.shade.app.data.local.entities.MessageEntity
import com.shade.app.data.local.entities.MessageStatus
import com.shade.app.ui.util.UiText
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

    // Çeviri dialog state
    var pendingTranslationMessageId by remember { mutableStateOf<String?>(null) }
    var pendingTranslationContent by remember { mutableStateOf("") }
    var showLanguageDialog by remember { mutableStateOf(false) }

    // Dil seçim dialogu
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Dil Seçin") },
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

    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (listState.firstVisibleItemIndex <= 1){
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
    Scaffold(
        topBar = {
            if (uiState.isSearchActive) {
                // Arama modu TopAppBar
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChange(it) },
                            placeholder = { Text("Mesajlarda ara...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.toggleSearch() }) {
                            Icon(Icons.Default.Close, contentDescription = "Aramayı Kapat")
                        }
                    }
                )
            } else {
                // Normal TopAppBar
                TopAppBar(
                    title = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onProfileClick(uiState.chatId) }
                        ) {
                            Text(
                                text = uiState.chatName,
                                style = MaterialTheme.typography.titleMedium
                            )
                            val subtitle = uiState.lastSeenText.ifBlank { "Profil detayları için tıkla" }
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (uiState.lastSeenText == "Çevrimiçi")
                                    Color(0xFF4CAF50)
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.toggleSearch() }) {
                            Icon(Icons.Default.Search, contentDescription = "Mesajlarda Ara")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isSearchActive && uiState.searchQuery.isNotBlank()) {
                // Arama modu: sonuçları göster
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.searchResults, key = { it.messageId }) { message ->
                            MessageItem(
                                message = message,
                                isMe = message.senderId == uiState.myShadeId,
                                translatedText = uiState.translatedMessages[message.messageId],
                                isTranslating = uiState.translatingMessageId == message.messageId,
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
                // Normal mesaj listesi
                val reversedMessages = remember(uiState.messages) { uiState.messages.reversed() }
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    reverseLayout = true
                ) {
                    items(
                        items = reversedMessages,
                        key = { it.messageId }
                    ) { message ->
                        MessageItem(
                            message = message,
                            isMe = message.senderId == uiState.myShadeId,
                            translatedText = uiState.translatedMessages[message.messageId],
                            isTranslating = uiState.translatingMessageId == message.messageId,
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

                // Mesaj yazma alanı
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Mesaj yaz...") },
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            viewModel.sendMessage(messageText)
                            messageText = ""
                        },
                        enabled = messageText.isNotBlank()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Gönder",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } // else sonu
        }
    }
}

@Composable
fun MessageItem(
    message: MessageEntity,
    isMe: Boolean,
    translatedText: String? = null,
    isTranslating: Boolean = false,
    onTranslateRequest: () -> Unit = {}
) {
    val dateFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeString = remember(message.timestamp) {
        dateFormatter.format(Date(message.timestamp))
    }
    val bubbleBg = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (isMe) Color.White else MaterialTheme.colorScheme.onSecondaryContainer

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
            // Mesaj balonu
            Column(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMe) 16.dp else 0.dp,
                            bottomEnd = if (isMe) 0.dp else 16.dp
                        )
                    )
                    .background(bubbleBg)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .widthIn(max = 280.dp)
            ) {
                Text(text = message.content, color = textColor)

                // Çeviri yükleniyor
                if (isTranslating) {
                    Spacer(Modifier.height(6.dp))
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.CenterHorizontally),
                        strokeWidth = 2.dp,
                        color = if (isMe) Color.White else MaterialTheme.colorScheme.primary
                    )
                }

                // Çevrilmiş metin
                if (!translatedText.isNullOrBlank() && !isTranslating) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = if (isMe) Color.White.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.3f)
                    )
                    Text(
                        text = translatedText,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.85f),
                        fontStyle = FontStyle.Italic
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMe) Color.White.copy(alpha = 0.7f) else Color.Gray,
                    )
                    if (isMe) {
                        MessageStatusIcon(status = message.status)
                    }
                }
            }

            // 🌐 Çevir butonu (balonun altında, sadece ikon)
            IconButton(
                onClick = onTranslateRequest,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = "Çevir",
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun MessageStatusIcon(status: MessageStatus) {
    val icon = when (status) {
        MessageStatus.PENDING -> Icons.Default.AccessTime
        MessageStatus.SENT -> Icons.Default.Check
        MessageStatus.DELIVERED, MessageStatus.READ -> Icons.Default.DoneAll
        MessageStatus.FAILED -> Icons.Default.ErrorOutline
    }

    val tint = when (status) {
        MessageStatus.READ -> Color(0xFF00B2FF)
        else -> Color.White.copy(alpha = 0.7f)
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
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = UiText.StringResource(R.string.unread_messages).asString(),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}