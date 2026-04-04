package com.shade.app.ui.home

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shade.app.data.local.entities.ChatEntity
import com.shade.app.data.local.model.ChatWithContact
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "SHADE_HOME"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onChatClick: (String, String) -> Unit,
    onNavigateToContacts: () -> Unit,
    onLogout: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lookupState by viewModel.lookupState.collectAsState()
    val loggedOut by viewModel.loggedOut.collectAsState()

    LaunchedEffect(loggedOut) {
        if (loggedOut) onLogout()
    }

    var showLookupDialog by remember { mutableStateOf(false) }
    var shadeIdInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        Log.d(TAG, "HomeScreen açıldı")
    }

    DisposableEffect(Unit) {
        onDispose {
            Log.d(TAG, "HomeScreen kapandı")
        }
    }

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
                Log.d(TAG, "Yeni mesaj dialogu kapatıldı")
                viewModel.resetLookupState()
            },
            title = { Text("Yeni Mesaj") },
            text = {
                Column {
                    Text(
                        text = "Mesaj göndermek istediğin kişinin Shade ID'sini gir kanka.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = shadeIdInput,
                        onValueChange = { shadeIdInput = it },
                        label = { Text("Shade ID") },
                        placeholder = { Text("Örn: CG-####-####") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = lookupState is LookupUiState.Error
                    )
                    if (lookupState is LookupUiState.Error) {
                        Text(
                            text = (lookupState as LookupUiState.Error).message.asString(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        Log.d(TAG, "Ara ve Başlat butonuna tıklandı: $shadeIdInput")
                        viewModel.startLookup(shadeIdInput, onChatClick)
                    },
                    enabled = shadeIdInput.isNotBlank() && lookupState !is LookupUiState.Loading
                ) {
                    if (lookupState is LookupUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("Ara ve Başlat")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    Log.d(TAG, "Yeni mesaj dialogu İptal ile kapatıldı")
                    showLookupDialog = false
                }) {
                    Text("İptal")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shade") },
                actions = {
                    IconButton(onClick = {
                        Log.d(TAG, "Kişiler butonuna tıklandı → Contacts ekranına geçiliyor")
                        onNavigateToContacts()
                    }) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = "Kişiler"
                        )
                    }
                    IconButton(onClick = {
                        Log.d(TAG, "Çıkış butonuna tıklandı")
                        viewModel.logout()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Çıkış Yap"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                Log.d(TAG, "Yeni mesaj FAB tıklandı → dialog açılıyor")
                showLookupDialog = true
            }) {
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
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.chats.isEmpty()) {
                Text(
                    text = "Henüz mesajın yok. Karanlıkta bir ışık yak!",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.chats, key = { it.displayName }) { chat ->
                        ChatItem(
                            chat = chat,
                            onClick = {
                                Log.d(TAG, "Sohbete tıklandı: chatId=${chat.chat.chatId}, name=${chat.displayName}")
                                onChatClick(chat.chat.chatId, chat.displayName)
                            },
                            onDelete = {
                                Log.d(TAG, "Sohbet silme: chatId=${chat.chat.chatId}")
                                viewModel.deleteChat(chat)
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }

            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(50.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = chat.displayName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = chat.displayName,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatTimestamp(chat.chat.lastMessageTimestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = chat.chat.lastMessage ?: "Mesaj yok",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (chat.chat.unreadCount > 0) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(chat.chat.unreadCount.toString())
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(date)
}