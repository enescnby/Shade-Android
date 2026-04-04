package com.shade.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey
    val chatId: String,
    val lastMessage: String?,
    val lastMessageTimestamp: Long,
    val unreadCount: Int = 0,
)