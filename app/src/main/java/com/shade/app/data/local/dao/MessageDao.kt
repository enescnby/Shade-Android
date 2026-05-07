package com.shade.app.data.local.dao

import androidx.paging.PagingSource
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

    /**
     * Sayfalı mesaj listesi — en yeni mesajlar önce gelir.
     * [LazyPagingItems] ile ChatScreen'de kullanılır.
     * PAGE_SIZE = 40 mesaj yüklenir; kaydırdıkça önceki mesajlar eklenir.
     */
    @Query("SELECT * FROM messages WHERE senderId = :chatId OR receiverId = :chatId ORDER BY timestamp DESC")
    fun getMessagesForChatPaged(chatId: String): PagingSource<Int, MessageEntity>

    @Query("SELECT * FROM messages WHERE senderId = :chatId AND status != 'READ'")
    suspend fun getUnreadMessages(chatId: String): List<MessageEntity>

    @Query("UPDATE messages SET status = :status WHERE messageId = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus)

    @Query("UPDATE messages SET imagePath = :path WHERE messageId = :messageId")
    suspend fun updateImagePath(messageId: String, path: String)

    @Query("UPDATE messages SET audioPath = :path, audioDurationMs = :durationMs WHERE messageId = :messageId")
    suspend fun updateAudioPath(messageId: String, path: String, durationMs: Long)

    @Query("UPDATE messages SET filePath = :path WHERE messageId = :messageId")
    suspend fun updateFilePath(messageId: String, path: String)

    @Query("SELECT status FROM messages WHERE messageId = :messageId")
    suspend fun getMessageStatus(messageId: String): MessageStatus?

    @Query("SELECT * FROM messages WHERE (senderId = :chatId OR receiverId = :chatId) AND content LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchMessages(chatId: String, query: String): Flow<List<MessageEntity>>

    @Delete
    suspend fun deleteMessage(message: MessageEntity)

    @Query("UPDATE messages SET isDeleted = 1 WHERE messageId = :messageId")
    suspend fun markAsDeleted(messageId: String)

    @Query("UPDATE messages SET content = :content, isEdited = 1 WHERE messageId = :messageId")
    suspend fun updateContent(messageId: String, content: String)

    @Query("SELECT COUNT(*) FROM messages WHERE (senderId = :chatId OR receiverId = :chatId) AND messageType = 'IMAGE' AND isDeleted = 0")
    suspend fun countMediaMessages(chatId: String): Int

    /** Deletes non-deleted messages older than [cutoffMs] for the given chat.
     *  Returns the number of rows deleted. */
    @Query("""
        DELETE FROM messages
        WHERE (senderId = :chatId OR receiverId = :chatId)
          AND timestamp < :cutoffMs
          AND isDeleted = 0
    """)
    suspend fun deleteMessagesOlderThan(chatId: String, cutoffMs: Long): Int

    /**
     * Auto-delete için: sadece [enabledAtMs] sonrasında gelen ve [cutoffMs]'den
     * eski mesajları siler. Özellik açılmadan önceki mesajlara dokunmaz.
     */
    @Query("""
        DELETE FROM messages
        WHERE (senderId = :chatId OR receiverId = :chatId)
          AND timestamp >= :enabledAtMs
          AND timestamp < :cutoffMs
          AND isDeleted = 0
    """)
    suspend fun deleteExpiredMessagesAfter(chatId: String, enabledAtMs: Long, cutoffMs: Long): Int
}
