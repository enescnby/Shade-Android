package com.shade.app.domain.usecase.message

import com.shade.app.data.local.entities.MessageStatus
import com.shade.app.domain.repository.MessageRepository
import com.shade.app.proto.DeliveryReceipt
import com.shade.app.proto.ReceiptStatus
import javax.inject.Inject

class HandleIncomingReceiptUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
){
    suspend operator fun invoke(receipt: DeliveryReceipt) {
        val newStatus = when (receipt.status) {
            ReceiptStatus.DELIVERED -> MessageStatus.DELIVERED
            ReceiptStatus.READ -> MessageStatus.READ
            else -> MessageStatus.SENT
        }

        messageRepository.updateMessageStatusIfForward(receipt.messageId, newStatus)
    }
}
