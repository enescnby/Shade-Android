package com.shade.app.ui.group

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shade.app.data.local.entities.GroupEntity
import com.shade.app.data.local.entities.GroupMemberEntity
import com.shade.app.domain.repository.GroupRepository
import com.shade.app.security.KeyVaultManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupDetailUiState(
    val group: GroupEntity? = null,
    val members: List<GroupMemberEntity> = emptyList(),
    val myUserId: String = "",
    val isOwner: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val inviteCode: String? = null,
    val leftGroup: Boolean = false,
) {
    val memberCount: Int get() = members.size
}

/**
 * Backs `GroupDetailScreen`.
 *
 * Member list + group metadata are observed live from the local cache so any
 * `GroupMembershipEvent` immediately reflects in the UI. A best-effort REST
 * refresh runs on init to fill stale shade_ids and pick up server-side
 * changes (avatar, name).
 */
@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val keyVaultManager: KeyVaultManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val groupId: String = savedStateHandle["groupId"] ?: ""

    private val _uiState = MutableStateFlow(GroupDetailUiState())
    val uiState: StateFlow<GroupDetailUiState> = _uiState

    init {
        viewModelScope.launch {
            val me = keyVaultManager.getUserId().orEmpty()
            _uiState.update { it.copy(myUserId = me) }
        }
        observeLocal()
        refresh()
    }

    private fun observeLocal() {
        combine(
            groupRepository.observeCachedGroup(groupId),
            groupRepository.observeCachedMembers(groupId),
        ) { group, members -> group to members }
            .onEach { (group, members) ->
                _uiState.update { st ->
                    st.copy(
                        group = group,
                        members = members.sortedWith(
                            compareByDescending<GroupMemberEntity> { it.role == "owner" }
                                .thenBy { it.shadeId.ifBlank { it.userId } }
                        ),
                        isOwner = group?.ownerId == st.myUserId,
                        isLoading = false,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun refresh() {
        viewModelScope.launch {
            groupRepository.getGroup(groupId).onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun createInvite() {
        viewModelScope.launch {
            groupRepository.createInvite(groupId = groupId, maxUses = 5).fold(
                onSuccess = { resp -> _uiState.update { it.copy(inviteCode = resp.code) } },
                onFailure = { e -> _uiState.update { it.copy(error = e.message) } },
            )
        }
    }

    fun dismissInvite() {
        _uiState.update { it.copy(inviteCode = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun removeMember(userId: String) {
        if (!_uiState.value.isOwner) return
        if (userId == _uiState.value.myUserId) return
        viewModelScope.launch {
            groupRepository.removeMember(groupId, userId).onFailure { e ->
                _uiState.update { it.copy(error = e.message ?: "Üye çıkarılamadı") }
            }
        }
    }

    fun leaveGroup() {
        val me = _uiState.value.myUserId
        if (me.isEmpty()) return
        viewModelScope.launch {
            groupRepository.removeMember(groupId, me).fold(
                onSuccess = { _uiState.update { it.copy(leftGroup = true) } },
                onFailure = { e -> _uiState.update { it.copy(error = e.message ?: "Gruptan çıkılamadı") } },
            )
        }
    }

    fun deleteGroup() {
        if (!_uiState.value.isOwner) return
        viewModelScope.launch {
            groupRepository.deleteGroup(groupId).fold(
                onSuccess = { _uiState.update { it.copy(leftGroup = true) } },
                onFailure = { e -> _uiState.update { it.copy(error = e.message ?: "Grup silinemedi") } },
            )
        }
    }
}
