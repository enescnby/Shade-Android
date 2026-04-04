package com.shade.app.domain.repository

import com.shade.app.data.local.entities.ChatEntity
import com.shade.app.data.local.model.ChatWithContact
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getAllChats(): Flow<List<ChatEntity>>

    fun getAllChatsWithContact(): Flow<List<ChatWithContact>>
    fun observeChatWithContact(chatId: String): Flow<ChatWithContact?>
    suspend fun insertOrUpdateChat(chat: ChatEntity)
    suspend fun resetUnreadCount(chatId: String)
    suspend fun updateLastMessage(chatId: String, lastMessage: String, timestamp: Long)
    suspend fun updateChatWithNewMessage(chatId: String, lastMessage: String, timestamp: Long)
    suspend fun deleteChat(chatId: String)
}