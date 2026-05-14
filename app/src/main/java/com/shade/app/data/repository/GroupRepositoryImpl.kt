package com.shade.app.data.repository

import com.shade.app.data.remote.api.GroupService
import com.shade.app.data.remote.dto.AddMemberRequest
import com.shade.app.data.remote.dto.CreateGroupRequest
import com.shade.app.data.remote.dto.CreateInviteRequest
import com.shade.app.data.remote.dto.GroupResponse
import com.shade.app.data.remote.dto.InviteResponse
import com.shade.app.data.remote.dto.RedeemInviteResponse
import com.shade.app.domain.repository.GroupRepository
import com.shade.app.security.KeyVaultManager
import javax.inject.Inject

class GroupRepositoryImpl @Inject constructor(
    private val groupService: GroupService,
    private val keyVaultManager: KeyVaultManager,
) : GroupRepository {

    private suspend fun token() = "Bearer ${keyVaultManager.getAccessToken()}"

    override suspend fun createGroup(name: String, memberIds: List<String>): Result<GroupResponse> =
        runCatching {
            val resp = groupService.createGroup(token(), CreateGroupRequest(name, memberIds))
            resp.body() ?: error("Empty response (${resp.code()})")
        }

    override suspend fun listGroups(): Result<List<GroupResponse>> =
        runCatching {
            val resp = groupService.listGroups(token())
            resp.body() ?: error("Empty response (${resp.code()})")
        }

    override suspend fun getGroup(groupId: String): Result<GroupResponse> =
        runCatching {
            val resp = groupService.getGroup(token(), groupId)
            resp.body() ?: error("Empty response (${resp.code()})")
        }

    override suspend fun deleteGroup(groupId: String): Result<Unit> =
        runCatching {
            val resp = groupService.deleteGroup(token(), groupId)
            if (!resp.isSuccessful) error("Delete failed (${resp.code()})")
        }

    override suspend fun addMember(groupId: String, userId: String): Result<Unit> =
        runCatching {
            val resp = groupService.addMember(token(), groupId, AddMemberRequest(userId))
            if (!resp.isSuccessful) error("Add member failed (${resp.code()})")
        }

    override suspend fun removeMember(groupId: String, userId: String): Result<Unit> =
        runCatching {
            val resp = groupService.removeMember(token(), groupId, userId)
            if (!resp.isSuccessful) error("Remove member failed (${resp.code()})")
        }

    override suspend fun createInvite(groupId: String?, maxUses: Int): Result<InviteResponse> =
        runCatching {
            val resp = groupService.createInvite(token(), CreateInviteRequest(groupId, maxUses))
            resp.body() ?: error("Empty response (${resp.code()})")
        }

    override suspend fun redeemInvite(code: String): Result<RedeemInviteResponse> =
        runCatching {
            val resp = groupService.redeemInvite(token(), code)
            resp.body() ?: error("Empty response (${resp.code()})")
        }
}
