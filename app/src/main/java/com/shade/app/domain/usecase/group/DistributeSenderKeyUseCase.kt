package com.shade.app.domain.usecase.group

import android.util.Log
import com.shade.app.data.local.entities.OwnSenderKeyEntity
import com.shade.app.domain.repository.GroupRepository
import com.shade.app.domain.repository.SenderKeyRepository
import com.shade.app.security.KeyVaultManager
import javax.inject.Inject

/**
 * Ships the caller's current SKDM to every group member except self.
 *
 * If [force] is `false` (default), members whose `(group, peerUser, ownKeyId)`
 * tuple is already marked as dispatched are skipped — that way reconnecting
 * doesn't re-spam SKDMs. After rotation the caller can purge stale dispatch
 * records and re-distribute the new key.
 */
class DistributeSenderKeyUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
    private val senderKeyRepository: SenderKeyRepository,
    private val sendSkdm: SendSenderKeyDistributionUseCase,
    private val keyVaultManager: KeyVaultManager,
) {
    suspend operator fun invoke(
        ownKey: OwnSenderKeyEntity,
        force: Boolean = false,
        onlyUserId: String? = null,
    ): Int {
        val myUserId = keyVaultManager.getUserId() ?: return 0

        // Prefer the local cache; fall back to a network refresh if empty.
        val cached = groupRepository.getCachedMembers(ownKey.groupId)
        val members = if (cached.isNotEmpty()) cached else {
            groupRepository.getGroup(ownKey.groupId).getOrNull()
                ?.members?.map { resp ->
                    com.shade.app.data.local.entities.GroupMemberEntity(
                        groupId = ownKey.groupId,
                        userId = resp.userId,
                        shadeId = resp.shadeId,
                        role = resp.role,
                    )
                } ?: emptyList()
        }

        var sent = 0
        for (member in members) {
            if (member.userId == myUserId) continue
            if (onlyUserId != null && member.userId != onlyUserId) continue
            // We don't track other devices per user yet; route by user_id and
            // let all recipient devices install the same chain. The proto's
            // `recipient_device_id` is informational for future per-device
            // routing.
            val peerDeviceId = member.userId
            val already = !force && senderKeyRepository.isSkdmDispatched(
                groupId = ownKey.groupId,
                peerUserId = member.userId,
                peerDeviceId = peerDeviceId,
                ownKeyId = ownKey.keyId,
            )
            if (already) continue
            sendSkdm(ownKey, recipientUserId = member.userId, recipientDeviceId = "")
                .onSuccess { sent++ }
                .onFailure {
                    Log.w(TAG, "SKDM to ${member.userId} failed: ${it.message}")
                }
        }
        return sent
    }

    private companion object {
        private const val TAG = "DistSKDM"
    }
}
