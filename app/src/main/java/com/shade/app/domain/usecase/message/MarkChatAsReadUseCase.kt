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

        unreadMessages.forEach { msg ->
            messageRepository.updateMessageStatus(msg.messageId, MessageStatus.READ)
        }

        for (msg in unreadMessages) {
            sendReceiptUseCase(msg.messageId, chatId, MessageStatus.READ)
        }

        sendReceiptUseCase.sendBatchReadReceipts(unreadMessages.map { it.messageId })

        chatRepository.resetUnreadCount(chatId)
    }
}
