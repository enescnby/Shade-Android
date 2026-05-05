package com.shade.app.ui.group

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shade.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    onBack: () -> Unit,
    onGroupCreated: (groupId: String, groupName: String) -> Unit,
    viewModel: CreateGroupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // Navigate when created
    LaunchedEffect(uiState.createdGroupId) {
        val gid = uiState.createdGroupId
        if (gid != null) {
            // groupName will be retrieved from local DB, but we pass the form field value
            onGroupCreated(gid, gid) // caller can read from DB; simplified for now
        }
    }

    var groupName by remember { mutableStateOf("") }
    // Simple multi-field for adding member user IDs (comma-separated or one per line)
    var memberInput by remember { mutableStateOf("") }
    val members = remember { mutableStateListOf<String>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.create_group_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.createGroup(groupName, members.toList()) },
                        enabled = groupName.isNotBlank() && !uiState.isLoading,
                    ) {
                        Text(stringResource(R.string.create_group_action))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text(stringResource(R.string.group_name_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))

            // Member add row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = memberInput,
                    onValueChange = { memberInput = it },
                    label = { Text(stringResource(R.string.group_member_user_id_hint)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val id = memberInput.trim()
                        if (id.isNotEmpty() && !members.contains(id)) {
                            members.add(id)
                            memberInput = ""
                        }
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add member")
                }
            }

            Spacer(Modifier.height(8.dp))

            if (members.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.group_members_header),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
                LazyColumn {
                    items(members) { uid ->
                        ListItem(
                            headlineContent = { Text(uid) },
                            trailingContent = {
                                IconButton(onClick = { members.remove(uid) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove")
                                }
                            }
                        )
                    }
                }
            }

            uiState.error?.let { err ->
                Spacer(Modifier.height(8.dp))
                Text(err, color = MaterialTheme.colorScheme.error)
            }

            if (uiState.isLoading) {
                Spacer(Modifier.height(16.dp))
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}
