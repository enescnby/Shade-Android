package com.shade.app.data.repository

import com.shade.app.data.local.dao.MessageDao
import com.shade.app.data.local.entities.MessageEntity
import com.shade.app.data.local.entities.MessageStatus
import com.shade.app.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MessageRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao
) : MessageRepository {
    override suspend fun insertMessage(message: MessageEntity) {
        messageDao.insertMessage(message)
    }

    override fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>> {
        return messageDao.getMessagesForChat(chatId)
    }

    override suspend fun updateMessageStatus(messageId: String, status: MessageStatus) {
        messageDao.updateMessageStatus(messageId, status)
    }

    override suspend fun deleteMessage(message: MessageEntity) {
        messageDao.deleteMessage(message)
    }
}