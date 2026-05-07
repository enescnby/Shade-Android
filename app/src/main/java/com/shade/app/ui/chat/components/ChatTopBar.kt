package com.shade.app.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shade.app.ui.theme.*

@Composable
fun ChatTopBar(
    chatName: String,
    chatId: String,
    lastSeenText: String,
    autoDeleteMinutes: Int,
    isSearchActive: Boolean,
    searchQuery: String,
    onBackClick: () -> Unit,
    onProfileClick: (String) -> Unit,
    onSearchToggle: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onShowBgPicker: () -> Unit,
    onShowAutoDeletePicker: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
        if (isSearchActive) {
            SearchBar(
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onClose = onSearchToggle
            )
        } else {
            NormalBar(
                chatName = chatName,
                chatId = chatId,
                lastSeenText = lastSeenText,
                autoDeleteMinutes = autoDeleteMinutes,
                onBackClick = onBackClick,
                onProfileClick = onProfileClick,
                onSearchToggle = onSearchToggle,
                onShowBgPicker = onShowBgPicker,
                onShowAutoDeletePicker = onShowAutoDeletePicker
            )
        }
    }
}

@Composable
private fun SearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = "Aramayı Kapat", tint = MaterialTheme.colorScheme.onSurface)
        }
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Mesajlarda ara...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentPurple,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = AccentPurple
            )
        )
    }
}

@Composable
private fun NormalBar(
    chatName: String,
    chatId: String,
    lastSeenText: String,
    autoDeleteMinutes: Int,
    onBackClick: () -> Unit,
    onProfileClick: (String) -> Unit,
    onSearchToggle: () -> Unit,
    onShowBgPicker: () -> Unit,
    onShowAutoDeletePicker: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = MaterialTheme.colorScheme.onSurface)
        }

        Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = BubbleMine) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = chatName.take(1).uppercase(),
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
                .clickable { onProfileClick(chatId) }
        ) {
            Text(chatName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            val subtitle = lastSeenText.ifBlank { "Profil detayları" }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (lastSeenText == "Çevrimiçi") Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onSearchToggle) {
            Icon(Icons.Default.Search, contentDescription = "Mesajlarda Ara", tint = MaterialTheme.colorScheme.onSurface)
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Seçenekler", tint = MaterialTheme.colorScheme.onSurface)
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                DropdownMenuItem(
                    text = { Text("Arkaplan Rengi", color = MaterialTheme.colorScheme.onSurface) },
                    leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null, tint = AccentPurple) },
                    onClick = { showMenu = false; onShowBgPicker() }
                )
                DropdownMenuItem(
                    text = {
                        val label = if (autoDeleteMinutes == 0) "Otomatik Silme: Kapalı" else "Otomatik Silme: Açık"
                        Text(label, color = MaterialTheme.colorScheme.onSurface)
                    },
                    leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null, tint = AccentPurple) },
                    onClick = { showMenu = false; onShowAutoDeletePicker() }
                )
            }
        }
    }
}
