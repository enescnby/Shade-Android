package com.shade.app.domain.usecase.message

import android.util.Base64
import android.util.Log
import com.google.protobuf.ByteString
import com.shade.app.data.local.dao.ContactDao
import com.shade.app.data.local.entities.MessageStatus
import com.shade.app.data.remote.api.MessageService
import com.shade.app.data.remote.dto.InboxMessageDto
import com.shade.app.data.remote.dto.InboxReceiptDto
import com.shade.app.domain.repository.MessageRepository
import com.shade.app.proto.EncryptedPayload
import com.shade.app.proto.MessageType
import com.shade.app.security.KeyVaultManager
import javax.inject.Inject

/**
 * Drains the per-device RabbitMQ queue via `GET /messages/inbox` and dispatches
 * each item to the right local use case:
 *  - 1-to-1 messages   → [ReceiveMessageUseCase]
 *  - Group messages    → [ReceiveGroupMessageUseCase]  (Sender Keys ratchet)
 *  - Receipts          → bumps the local message status (forward-only)
 *
 * The server clears the queue as part of this call, so each item arrives at
 * most once per drain. Triggered both by FCM wake-ups and on every WebSocket
 * (re)connect to catch anything that landed while offline.
 */
class FetchInboxUseCase @Inject constructor(
    private val messageService: MessageService,
    private val keyVaultManager: KeyVaultManager,
    private val contactDao: ContactDao,
    private val receiveMessageUseCase: ReceiveMessageUseCase,
    private val receiveGroupMessageUseCase: ReceiveGroupMessageUseCase,
    private val messageRepository: MessageRepository,
) {
    suspend operator fun invoke(limit: Int = DEFAULT_LIMIT) {
        try {
            val token = keyVaultManager.getAccessToken() ?: return
            val safeLimit = limit.coerceIn(1, MAX_LIMIT)
            while (true) {
                val response = messageService.getInbox("Bearer $token", safeLimit)

                if (!response.isSuccessful) {
                    Log.e(TAG, "HTTP ${response.code()}: ${response.message()}")
                    return
                }

                val body = response.body() ?: return
                Log.d(
                    TAG,
                    "Inbox batch: ${body.messages.size} message(s), ${body.receipts.size} receipt(s)"
                )
                if (body.messages.isEmpty() && body.receipts.isEmpty()) break

                body.messages.forEach { processMessage(it) }
                body.receipts.forEach { processReceipt(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch inbox: ${e.message}")
        }
    }

    private suspend fun processMessage(dto: InboxMessageDto) {
        try {
            val isGroup = !dto.groupId.isNullOrEmpty()

            if (isGroup) {
                // Inbox doesn't carry sender_shade_id. Resolve it now via
                // /keys/:id so the ReceiveGroupMessageUseCase persists a
                // proper shade_id (not the raw UUID) on MessageEntity —
                // downstream READ receipts route via shade_id.
                val resolvedShade = resolveSenderShadeId(dto.senderId)
                val payload = dto.toGroupPayload(senderShadeId = resolvedShade)
                receiveGroupMessageUseCase(payload, sendReceipt = true)
                return
            }

            // 1-to-1 — resolve sender_shade_id from local contact cache.
            // Special case: server can echo back messages that the user sent
            // from another device (e.g. the web client). For those,
            // sender_id == myUserId. We mark the payload with our own
            // shade_id so ReceiveMessageUseCase recognises the echo and
            // derives the conversation key with the receiver's pub key
            // instead of trying (and failing) to ECDH against ourselves.
            val myUserId = keyVaultManager.getUserId()
            val myShadeId = keyVaultManager.getShadeId()

            val senderShadeId = if (myUserId != null && myShadeId != null && dto.senderId == myUserId) {
                myShadeId
            } else {
                val contact = contactDao.getContactByUserId(dto.senderId)
                if (contact == null) {
                    Log.w(
                        TAG,
                        "No local contact for sender_id=${dto.senderId}, skipping ${dto.messageId}"
                    )
                    return
                }
                contact.shadeId
            }

            val payload = dto.toEncryptedPayload(senderShadeId = senderShadeId)
            receiveMessageUseCase(payload, sendReceipt = false)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process message ${dto.messageId}: ${e.message}")
        }
    }

    private suspend fun processReceipt(dto: InboxReceiptDto) {
        val status = when (dto.status) {
            "DELIVERED" -> MessageStatus.DELIVERED
            "READ" -> MessageStatus.READ
            else -> {
                Log.w(TAG, "Unknown receipt status '${dto.status}' for ${dto.messageId}")
                return
            }
        }
        messageRepository.updateMessageStatusIfForward(dto.messageId, status)
    }

    private fun InboxMessageDto.toEncryptedPayload(senderShadeId: String): EncryptedPayload {
        return EncryptedPayload.newBuilder()
            .setMessageId(messageId)
            .setSenderId(senderId)
            .setSenderShadeId(senderShadeId)
            .setReceiverId(receiverId.orEmpty())
            .setCiphertext(ByteString.copyFrom(Base64.decode(ciphertext, Base64.DEFAULT)))
            .setNonce(ByteString.copyFrom(Base64.decode(nonce, Base64.DEFAULT)))
            .setTimestamp(normalizeTimestamp(timestamp))
            .setType(if (messageType == 1) MessageType.IMAGE else MessageType.TEXT)
            .build()
    }

    /**
     * Build the proto for a group inbox item. The Sender Key fields are
     * required for decryption; if any are missing we still pass through and
     * let [ReceiveGroupMessageUseCase] buffer / drop with a clear log.
     */
    private fun InboxMessageDto.toGroupPayload(senderShadeId: String): EncryptedPayload {
        val builder = EncryptedPayload.newBuilder()
            .setMessageId(messageId)
            .setSenderId(senderId)
            .setSenderShadeId(senderShadeId)
            .setGroupId(groupId.orEmpty())
            .setCiphertext(ByteString.copyFrom(Base64.decode(ciphertext, Base64.DEFAULT)))
            .setNonce(ByteString.copyFrom(Base64.decode(nonce, Base64.DEFAULT)))
            .setTimestamp(normalizeTimestamp(timestamp))
            .setType(if (messageType == 1) MessageType.IMAGE else MessageType.TEXT)
        senderDeviceId?.let { builder.setSenderDeviceId(it) }
        senderKeyId?.let { builder.setSenderKeyId(it.toInt()) }
        chainIndex?.let { builder.setChainIndex(it) }
        signature?.let { builder.setSignature(ByteString.copyFrom(Base64.decode(it, Base64.DEFAULT))) }
        return builder.build()
    }

    /**
     * Resolve `sender_shade_id` for an inbox group message (the server doesn't
     * include it). Falls back to the empty string — ReceiveGroupMessageUseCase
     * has its own lookup and final fallback there, so we don't want to drop
     * the payload just because we couldn't resolve the contact here.
     */
    private suspend fun resolveSenderShadeId(senderUserId: String): String {
        val myUserId = keyVaultManager.getUserId()
        val myShadeId = keyVaultManager.getShadeId()
        if (myUserId != null && myShadeId != null && senderUserId == myUserId) {
            return myShadeId
        }
        return contactDao.getContactByUserId(senderUserId)?.shadeId.orEmpty()
    }

    /** Server may emit seconds (10 digits) or millis (13). Internally we use millis. */
    private fun normalizeTimestamp(ts: Long): Long =
        if (ts < SECONDS_THRESHOLD) ts * 1000L else ts

    private companion object {
        private const val TAG = "FetchInbox"
        private const val DEFAULT_LIMIT = 100
        private const val MAX_LIMIT = 500
        private const val SECONDS_THRESHOLD = 1_000_000_000_000L
    }
}
