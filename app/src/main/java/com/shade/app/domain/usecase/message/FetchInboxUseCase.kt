package com.shade.app.domain.usecase.message

import android.util.Base64
import android.util.Log
import com.google.protobuf.ByteString
import com.shade.app.data.local.entities.MessageStatus
import com.shade.app.data.local.dao.ContactDao
import com.shade.app.data.remote.api.MessageService
import com.shade.app.data.remote.dto.InboxMessageDto
import com.shade.app.data.remote.dto.InboxReceiptDto
import com.shade.app.domain.repository.MessageRepository
import com.shade.app.proto.EncryptedPayload
import com.shade.app.proto.MessageType
import com.shade.app.security.KeyVaultManager
import javax.inject.Inject

/**
 * Pulls queued messages + receipts in a single call from `GET /messages/inbox`.
 * The server clears the queue as part of this call, so we only get each item once.
 *
 * Triggered both by FCM wake-up (FetchMessagesWorker) and on every WebSocket (re)connect
 * to drain anything that landed while offline.
 */
class FetchInboxUseCase @Inject constructor(
    private val messageService: MessageService,
    private val keyVaultManager: KeyVaultManager,
    private val contactDao: ContactDao,
    private val receiveMessageUseCase: ReceiveMessageUseCase,
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(limit: Int = DEFAULT_LIMIT) {
        try {
            val token = keyVaultManager.getAccessToken() ?: return
            val safeLimit = limit.coerceIn(1, MAX_LIMIT)
            val response = messageService.getInbox("Bearer $token", safeLimit)

            if (!response.isSuccessful) {
                Log.e(TAG, "HTTP ${response.code()}: ${response.message()}")
                return
            }

            val body = response.body() ?: return
            Log.d(TAG, "Inbox drained: ${body.messages.size} message(s), ${body.receipts.size} receipt(s)")

            body.messages.forEach { processMessage(it) }
            body.receipts.forEach { processReceipt(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch inbox: ${e.message}")
        }
    }

    private suspend fun processMessage(dto: InboxMessageDto) {
        try {
            // The /inbox contract only returns sender_id (uuid), not sender_shade_id.
            // ReceiveMessageUseCase keys off sender_shade_id, so resolve it from the local
            // contact cache.
            //
            // Special case: server can echo back messages that the user sent from another
            // device (e.g. the web client). For those, sender_id == myUserId. We mark
            // the payload with our own shade_id so ReceiveMessageUseCase recognizes the
            // echo and derives the conversation key with the receiver's pub key instead
            // of trying (and failing) to ECDH against ourselves.
            val myUserId = keyVaultManager.getUserId()
            val myShadeId = keyVaultManager.getShadeId()

            val senderShadeId = if (myUserId != null && myShadeId != null && dto.senderId == myUserId) {
                myShadeId
            } else {
                val contact = contactDao.getContactByUserId(dto.senderId)
                if (contact == null) {
                    Log.w(TAG, "No local contact for sender_id=${dto.senderId}, skipping ${dto.messageId}")
                    return
                }
                contact.shadeId
            }

            val payload = dto.toEncryptedPayload(senderShadeId = senderShadeId)
            // Server already removed the message from the queue, so no extra REST receipt needed.
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
            .setReceiverId(receiverId)
            .setCiphertext(ByteString.copyFrom(Base64.decode(ciphertext, Base64.DEFAULT)))
            .setNonce(ByteString.copyFrom(Base64.decode(nonce, Base64.DEFAULT)))
            .setTimestamp(normalizeTimestamp(timestamp))
            .setType(if (messageType == 1) MessageType.IMAGE else MessageType.TEXT)
            .build()
    }

    /** Server may emit seconds (10 digits) or millis (13). Internally we use millis. */
    private fun normalizeTimestamp(ts: Long): Long =
        if (ts < SECONDS_THRESHOLD) ts * 1000L else ts

    companion object {
        private const val TAG = "FetchInbox"
        private const val DEFAULT_LIMIT = 100
        private const val MAX_LIMIT = 500
        private const val SECONDS_THRESHOLD = 1_000_000_000_000L
    }
}
