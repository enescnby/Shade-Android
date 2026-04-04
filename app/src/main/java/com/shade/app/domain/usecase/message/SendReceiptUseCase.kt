package com.shade.app.domain.usecase.message

import android.util.Log
import com.shade.app.data.local.entities.MessageStatus
import com.shade.app.data.remote.api.MessageService
import com.shade.app.data.remote.dto.BatchReceiptRequest
import com.shade.app.data.remote.dto.ReceiptRequest
import com.shade.app.domain.repository.ContactRepository
import com.shade.app.domain.repository.MessageRepository
import com.shade.app.proto.ReceiptStatus
import com.shade.app.proto.deliveryReceipt
import com.shade.app.proto.webSocketMessage
import com.shade.app.security.KeyVaultManager
import javax.inject.Inject

class SendReceiptUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
    private val contactRepository: ContactRepository,
    private val keyVaultManager: KeyVaultManager,
    private val messageService: MessageService
) {
    suspend operator fun invoke(messageId: String, receiverShadeId: String, status: MessageStatus) {
        val contact = contactRepository.getContactByShadeId(receiverShadeId) ?: return

        val protoStatus = when (status) {
            MessageStatus.READ -> ReceiptStatus.READ
            else -> ReceiptStatus.DELIVERED
        }

        // Try WebSocket first
        val socketMsg = webSocketMessage {
            receipt = deliveryReceipt {
                this.messageId = messageId
                senderId = keyVaultManager.getUserId() ?: return
                senderShadeId = keyVaultManager.getShadeId() ?: return
                receiverId = contact.userId
                this.status = protoStatus
                this.timestamp = System.currentTimeMillis()
            }
        }

        val sent = messageRepository.sendWebsocketMessage(socketMsg)
        if (sent) return

        // WebSocket failed — fallback to REST (only for READ, DELIVERED is handled server-side on REST path)
        if (status == MessageStatus.READ) {
            sendViaRest(messageId, "READ")
        }
    }

    suspend fun sendBatchReadReceipts(messageIds: List<String>) {
        if (messageIds.isEmpty()) return

        val receipts = messageIds.map { ReceiptRequest(messageId = it, status = "READ") }
        sendBatchViaRest(receipts)
    }

    private suspend fun sendViaRest(messageId: String, status: String) {
        sendBatchViaRest(listOf(ReceiptRequest(messageId = messageId, status = status)))
    }

    private suspend fun sendBatchViaRest(receipts: List<ReceiptRequest>) {
        try {
            val token = keyVaultManager.getAccessToken() ?: return
            val response = messageService.sendReceipts(
                "Bearer $token",
                BatchReceiptRequest(receipts)
            )
            if (!response.isSuccessful) {
                Log.e("SendReceipt", "REST receipt failed: HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("SendReceipt", "REST receipt error: ${e.message}")
        }
    }
}
