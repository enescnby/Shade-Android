package com.shade.app.domain.repository

import com.shade.app.data.local.entities.MessageEntity
import com.shade.app.data.local.entities.MessageStatus
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    suspend fun insertMessage(message: MessageEntity)
    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>>
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus)
    suspend fun deleteMessage(message: MessageEntity)
}