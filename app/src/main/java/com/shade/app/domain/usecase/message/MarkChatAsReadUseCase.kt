package com.shade.app.domain.usecase.message

import com.shade.app.data.local.entities.MessageStatus
import com.shade.app.domain.repository.ChatRepository
import com.shade.app.domain.repository.MessageRepository
import javax.inject.Inject

class MarkChatAsReadUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
    private val chatRepository: ChatRepository,
    private val sendReceiptUseCase: SendReceiptUseCase
) {
    suspend operator fun invoke(chatId: String) {
        val unreadMessages = messageRepository.getUnreadMessages(chatId)

        if (unreadMessages.isEmpty()) {
            chatRepository.resetUnreadCount(chatId)
            return
        }

        unreadMessages.forEach { message ->
            messageRepository.updateMessageStatus(message.messageId, MessageStatus.READ)
            sendReceiptUseCase(
                messageId = message.messageId,
                receiverShadeId = chatId,
                status = MessageStatus.READ
            )
        }

        chatRepository.resetUnreadCount(chatId)
    }
}