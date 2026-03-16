package com.shade.app.data.repository

import com.shade.app.data.local.dao.ChatDao
import com.shade.app.data.local.entities.ChatEntity
import com.shade.app.domain.repository.ChatRepository
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val chatDao: ChatDao
) : ChatRepository {
    override fun getAllChats() = chatDao.getAllChats()

    override suspend fun insertOrUpdateChat(chat: ChatEntity) {
        chatDao.insertOrUpdateChat(chat)
    }

    override suspend fun resetUnreadCount(chatId: String) {
        chatDao.resetUnreadCount(chatId)
    }

    override suspend fun deleteChat(chatId: String) {
        chatDao.deleteChat(chatId)
    }
}