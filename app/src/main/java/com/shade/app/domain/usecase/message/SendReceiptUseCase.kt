package com.shade.app.domain.usecase.message

import com.shade.app.data.local.entities.MessageStatus
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
    private val keyVaultManager: KeyVaultManager
) {
    suspend operator fun invoke(messageId: String, receiverShadeId: String, status: MessageStatus) {
        val contact = contactRepository.getContactByShadeId(receiverShadeId) ?: return

        val socketMsg = webSocketMessage {
            receipt = deliveryReceipt {
                this.messageId = messageId
                senderId = keyVaultManager.getUserId() ?: return
                senderShadeId = keyVaultManager.getShadeId() ?: return
                receiverId = contact.userId
                this.status = when(status) {
                    MessageStatus.READ -> ReceiptStatus.READ
                    else -> ReceiptStatus.DELIVERED
                }
                this.timestamp = System.currentTimeMillis()
            }
        }

        messageRepository.sendWebsocketMessage(socketMsg)
    }
}