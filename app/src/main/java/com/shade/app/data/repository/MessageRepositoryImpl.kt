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

    override suspend fun updateImagePath(messageId: String, path: String) {
        messageDao.updateImagePath(messageId, path)
    }

    override suspend fun getMessageStatus(messageId: String): MessageStatus? {
        return messageDao.getMessageStatus(messageId)
    }

    override suspend fun updateMessageStatusIfForward(messageId: String, newStatus: MessageStatus) {
        val currentStatus = messageDao.getMessageStatus(messageId) ?: return
        // FAILED is a terminal error state — receipts can still move it forward to DELIVERED/READ
        // because the server might have queued the message even if the WebSocket send returned false.
        // Only skip the update if we would go "backward" (e.g. DELIVERED → SENT).
        val effectiveCurrent = if (currentStatus == MessageStatus.FAILED) MessageStatus.PENDING else currentStatus
        if (newStatus.ordinal > effectiveCurrent.ordinal) {
            messageDao.updateMessageStatus(messageId, newStatus)
        }
    }

    override suspend fun deleteMessage(message: MessageEntity) = messageDao.deleteMessage(message)

    override fun searchMessages(chatId: String, query: String): Flow<List<MessageEntity>> =
        messageDao.searchMessages(chatId, query)

    override suspend fun markAsDeleted(messageId: String) = messageDao.markAsDeleted(messageId)

    override suspend fun updateMessageContent(messageId: String, content: String) =
        messageDao.updateContent(messageId, content)

    override suspend fun countMediaMessages(chatId: String): Int =
        messageDao.countMediaMessages(chatId)

    override suspend fun deleteMessagesOlderThan(chatId: String, cutoffMs: Long): Int =
        messageDao.deleteMessagesOlderThan(chatId, cutoffMs)
}
