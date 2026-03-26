package com.shade.app.data.repository

import com.shade.app.data.local.dao.MessageDao
import com.shade.app.data.local.entities.MessageEntity
import com.shade.app.data.local.entities.MessageStatus
import com.shade.app.data.remote.websocket.ShadeWebSocketManager
import com.shade.app.domain.repository.MessageRepository
import com.shade.app.proto.WebSocketMessage
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ShadeRepo"

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val webSocketManager: ShadeWebSocketManager
) : MessageRepository {

    override suspend fun insertMessage(message: MessageEntity) = messageDao.insertMessage(message)

    override fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>> = messageDao.getMessagesForChat(chatId)

    override suspend fun getUnreadMessages(chatId: String): List<MessageEntity> {
        return messageDao.getUnreadMessages(chatId)
    }

    override suspend fun updateMessageStatus(messageId: String, status: MessageStatus) = messageDao.updateMessageStatus(messageId, status)

    override suspend fun sendWebsocketMessage(message: WebSocketMessage): Boolean {
        return webSocketManager.sendMessage(message)
    }

    override fun observeIncomingMessages(): Flow<WebSocketMessage> {
        return webSocketManager.observeMessages()
    }

    override suspend fun deleteMessage(message: MessageEntity) = messageDao.deleteMessage(message)
}
