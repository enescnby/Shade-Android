package com.shade.app.data.repository

import com.shade.app.data.local.dao.ChatDao
import com.shade.app.data.local.entities.ChatEntity
import com.shade.app.data.local.model.ChatWithContact
import com.shade.app.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val chatDao: ChatDao
) : ChatRepository {
    override fun getAllChats() = chatDao.getAllChats()

    override fun getAllChatsWithContact(): Flow<List<ChatWithContact>> {
        return chatDao.getAllChatsWithContact()
    }

    override fun observeChatWithContact(chatId: String): Flow<ChatWithContact?> {
        return chatDao.observeChatWithContact(chatId)
    }

    override suspend fun insertOrUpdateChat(chat: ChatEntity) {
        chatDao.insertOrUpdateChat(chat)
    }

    override suspend fun resetUnreadCount(chatId: String) {
        chatDao.resetUnreadCount(chatId)
    }

    override suspend fun updateChatWithNewMessage(
        chatId: String,
        lastMessage: String,
        timestamp: Long
    ) {
        val updatedRows = chatDao.incrementUnreadCount(chatId, lastMessage, timestamp)
        if (updatedRows == 0) {
            chatDao.insertOrUpdateChat(ChatEntity(chatId, lastMessage, timestamp, 1))
        }
    }

    override suspend fun deleteChat(chatId: String) {
        chatDao.deleteChat(chatId)
    }
}