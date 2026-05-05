package com.shade.app.ui.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shade.app.data.local.entities.ChatEntity
import com.shade.app.domain.repository.ChatRepository
import com.shade.app.domain.repository.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateGroupUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val createdGroupId: String? = null,
)

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val chatRepository: ChatRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateGroupUiState())
    val uiState: StateFlow<CreateGroupUiState> = _uiState

    fun createGroup(name: String, memberIds: List<String>) {
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Group name cannot be empty")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = groupRepository.createGroup(name.trim(), memberIds)
            result.fold(
                onSuccess = { group ->
                    // Ensure a chat row exists locally for this group
                    chatRepository.insertOrUpdateChat(
                        ChatEntity(
                            chatId = group.groupId,
                            lastMessage = null,
                            lastMessageTimestamp = 0L,
                            isGroup = true,
                            groupName = group.name,
                        )
                    )
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        createdGroupId = group.groupId,
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to create group",
                    )
                }
            )
        }
    }
}
