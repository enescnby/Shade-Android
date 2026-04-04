package com.shade.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val messageId: String,

    val senderId: String,
    val receiverId: String,

    val messageType: MessageType,
    val content: String,

    val timestamp: Long,
    val status: MessageStatus
)

enum class MessageType {
    TEXT, IMAGE
}

enum class MessageStatus {
    PENDING, SENT, DELIVERED, READ, FAILED
}
