package com.shade.app.domain.repository

import com.shade.app.data.remote.dto.CreateGroupRequest
import com.shade.app.data.remote.dto.GroupResponse
import com.shade.app.data.remote.dto.InviteResponse
import com.shade.app.data.remote.dto.RedeemInviteResponse

interface GroupRepository {
    suspend fun createGroup(name: String, memberIds: List<String>): Result<GroupResponse>
    suspend fun listGroups(): Result<List<GroupResponse>>
    suspend fun getGroup(groupId: String): Result<GroupResponse>
    suspend fun deleteGroup(groupId: String): Result<Unit>
    suspend fun addMember(groupId: String, userId: String): Result<Unit>
    suspend fun removeMember(groupId: String, userId: String): Result<Unit>
    suspend fun createInvite(groupId: String? = null, maxUses: Int = 1): Result<InviteResponse>
    suspend fun redeemInvite(code: String): Result<RedeemInviteResponse>
}
