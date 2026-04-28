package com.shade.app.domain.repository

import com.shade.app.data.local.entities.MessageEntity
import com.shade.app.data.local.entities.MessageStatus
import com.shade.app.proto.WebSocketMessage
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    suspend fun insertMessage(message: MessageEntity)
    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>>
    suspend fun getUnreadMessages(chatId: String): List<MessageEntity>
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus)
    suspend fun sendWebsocketMessage(message: WebSocketMessage): Boolean
    fun observeIncomingMessages(): Flow<WebSocketMessage>
    suspend fun updateImagePath(messageId: String, path: String)
    suspend fun getMessageStatus(messageId: String): MessageStatus?
    suspend fun updateMessageStatusIfForward(messageId: String, newStatus: MessageStatus)
    suspend fun deleteMessage(message: MessageEntity)
    fun searchMessages(chatId: String, query: String): Flow<List<MessageEntity>>
}
