package com.shade.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shade.app.data.local.entities.MessageEntity
import com.shade.app.data.local.entities.MessageStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao{
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE senderId = :chatId OR receiverId = :chatId ORDER BY timestamp")
    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE senderId = :chatId AND status != 'READ'")
    suspend fun getUnreadMessages(chatId: String): List<MessageEntity>

    @Query("UPDATE messages SET status = :status WHERE messageId = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus)

    @Query("UPDATE messages SET imagePath = :path WHERE messageId = :messageId")
    suspend fun updateImagePath(messageId: String, path: String)

    @Query("SELECT status FROM messages WHERE messageId = :messageId")
    suspend fun getMessageStatus(messageId: String): MessageStatus?

    @Query("SELECT * FROM messages WHERE (senderId = :chatId OR receiverId = :chatId) AND content LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchMessages(chatId: String, query: String): Flow<List<MessageEntity>>

    @Delete
    suspend fun deleteMessage(message: MessageEntity)
}
