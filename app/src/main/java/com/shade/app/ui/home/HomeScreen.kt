package com.shade.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shade.app.data.local.model.ChatWithContact
import com.shade.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onChatClick: (String, String) -> Unit,
    onNavigateToContacts: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lookupState by viewModel.lookupState.collectAsState()

    var showLookupDialog by remember { mutableStateOf(false) }
    var shadeIdInput by remember { mutableStateOf("") }

    LaunchedEffect(lookupState) {
        if (lookupState is LookupUiState.Success) {
            showLookupDialog = false
            shadeIdInput = ""
            viewModel.resetLookupState()
        }
    }

    if (showLookupDialog) {
        AlertDialog(
            onDismissRequest = {
                viewModel.resetLookupState()
            },
            containerColor = SurfaceElevated,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    "Yeni Mesaj",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Mesaj göndermek istediğin kişinin Shade ID'sini gir.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = shadeIdInput,
                        onValueChange = { shadeIdInput = it },
                        label = { Text("Shade ID") },
                        placeholder = { Text("Örn: CG-####-####", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        isError = lookupState is LookupUiState.Error,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPurple,
                            unfocusedBorderColor = OutlineMuted,
                            focusedLabelColor = AccentPurple,
                            unfocusedLabelColor = TextMuted,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = AccentPurple
                        )
                    )
                    if (lookupState is LookupUiState.Error) {
                        Text(
                            text = (lookupState as LookupUiState.Error).message.asString(),
                            color = ErrorRed,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.startLookup(shadeIdInput, onChatClick) },
                    enabled = shadeIdInput.isNotBlank() && lookupState !is LookupUiState.Loading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentPurple
                    )
                ) {
                    if (lookupState is LookupUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("Ara ve Başlat", fontWeight = FontWeight.Medium)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showLookupDialog = false }) {
                    Text("İptal", color = TextSecondary)
                }
            }
        )
    }

    Scaffold(
        containerColor = RichBlack,
        topBar = {
            Surface(
                color = SurfaceDark,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Shade",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showLookupDialog = true },
                containerColor = AccentPurple,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.navigationBarsPadding()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Yeni Mesaj")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading && uiState.chats.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = AccentPurple
                )
            } else if (uiState.chats.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = TextMuted
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Henüz mesajın yok",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "Karanlıkta bir ışık yak!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(uiState.chats, key = { it.displayName }) { chat ->
                        ChatItem(
                            chat = chat,
                            onClick = {
                                onChatClick(chat.chat.chatId, chat.displayName)
                            },
                            onDelete = { viewModel.deleteChat(chat) }
                        )
                    }
                }
            }

            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    containerColor = SurfaceElevated,
                    contentColor = TextPrimary
                ) {
                    Text(error)
                }
            }
        }
    }
}

@Composable
fun ChatItem(
    chat: ChatWithContact,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with gradient
            Surface(
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                color = BubbleMine
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = chat.displayName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = formatTimestamp(chat.chat.lastMessageTimestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (chat.chat.unreadCount > 0) AccentPurple else TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = chat.chat.lastMessage ?: "Mesaj yok",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (chat.chat.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = AccentPurple,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = chat.chat.unreadCount.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Calendar.getInstance()
    val msgCal = Calendar.getInstance().apply { time = date }

    return if (now.get(Calendar.DATE) == msgCal.get(Calendar.DATE) &&
        now.get(Calendar.MONTH) == msgCal.get(Calendar.MONTH) &&
        now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR)
    ) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    } else {
        SimpleDateFormat("dd MMM", Locale.getDefault()).format(date)
    }
}
