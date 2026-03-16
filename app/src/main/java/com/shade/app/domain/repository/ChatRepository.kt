package com.shade.app.domain.repository

import com.shade.app.data.local.entities.ChatEntity
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getAllChats(): Flow<List<ChatEntity>>
    suspend fun insertOrUpdateChat(chat: ChatEntity)
    suspend fun resetUnreadCount(chatId: String)
    suspend fun deleteChat(chatId: String)
}