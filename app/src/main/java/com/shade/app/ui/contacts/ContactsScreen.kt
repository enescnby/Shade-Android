package com.shade.app.ui.contacts

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shade.app.data.local.entities.ContactEntity

private const val TAG = "SHADE_CONTACTS"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    onBackClick: () -> Unit,
    onContactClick: (String, String) -> Unit,
    viewModel: ContactsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        Log.d(TAG, "ContactsScreen açıldı")
    }

    DisposableEffect(Unit) {
        onDispose {
            Log.d(TAG, "ContactsScreen kapandı")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kişiler") },
                navigationIcon = {
                    IconButton(onClick = {
                        Log.d(TAG, "Geri butonuna tıklandı → Home'a dönülüyor")
                        onBackClick()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Kişi ara...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.contacts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (uiState.searchQuery.isBlank()) "Henüz hiç kişin yok."
                        else "Kişi bulunamadı.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.contacts, key = { it.userId }) { contact ->
                        ContactItem(
                            contact = contact,
                            onClick = {
                                val displayName = contact.savedName ?: contact.shadeId
                                Log.d(TAG, "Kişiye tıklandı: ${contact.shadeId} ($displayName) → Chat açılıyor")
                                onContactClick(contact.shadeId, displayName)
                            },
                            onDelete = {
                                Log.d(TAG, "Kişi silme isteği: ${contact.shadeId}")
                                viewModel.deleteContact(contact)
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
        }
    }
}

@Composable
fun ContactItem(
    contact: ContactEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val displayName = contact.savedName ?: contact.shadeId

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Kişiyi Sil") },
            text = { Text("$displayName kişisini silmek istediğine emin misin?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) { Text("Sil", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = {
                    Log.d("SHADE_CONTACTS", "Silme dialogu iptal edildi: $displayName")
                    showDeleteDialog = false
                }) { Text("İptal") }
            }
        )
    }

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
                    text = displayName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = contact.shadeId,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = {
            Log.d("SHADE_CONTACTS", "Silme ikonu tıklandı: $displayName")
            showDeleteDialog = true
        }) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Sil",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}