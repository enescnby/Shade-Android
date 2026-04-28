package com.shade.app.ui.home

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Security
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

private const val TAG = "SHADE_HOME"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onChatClick: (String, String) -> Unit,
    onNavigateToContacts: () -> Unit,
    onLogout: () -> Unit = {},
    onSecurityAuditClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val loggedOut by viewModel.loggedOut.collectAsState()

    LaunchedEffect(Unit) {
        Log.d(TAG, "HomeScreen açıldı")
    }

    LaunchedEffect(loggedOut) {
        if (loggedOut) onLogout()
    }

    DisposableEffect(Unit) {
        onDispose { Log.d(TAG, "HomeScreen kapandı") }
    }

    Scaffold(
        containerColor = RichBlack,
        topBar = {
            TopAppBar(
                title = { Text("Shade") },
                actions = {
                    IconButton(onClick = {
                        Log.d(TAG, "Kişiler butonuna tıklandı")
                        onNavigateToContacts()
                    }) {
                        Icon(Icons.Default.People, contentDescription = "Kişiler")
                    }
                    IconButton(onClick = {
                        Log.d(TAG, "Güvenlik Günlüğü butonuna tıklandı")
                        onSecurityAuditClick()
                    }) {
                        Icon(Icons.Default.Security, contentDescription = "Hesap Etkinliği")
                    }
                    IconButton(onClick = {
                        Log.d(TAG, "Çıkış butonuna tıklandı")
                        viewModel.logout()
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Çıkış Yap")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    Log.d(TAG, "Yeni mesaj FAB tıklandı → Kişiler ekranına geçiliyor")
                    onNavigateToContacts()
                },
                containerColor = AccentPurple,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.navigationBarsPadding()
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Yeni Mesaj")
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
                                Log.d(TAG, "Sohbete tıklandı: ${chat.chat.chatId}")
                                onChatClick(chat.chat.chatId, chat.displayName)
                            },
                            onDelete = {
                                Log.d(TAG, "Sohbet silme: ${chat.chat.chatId}")
                                viewModel.deleteChat(chat)
                            }
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
