package com.shade.app.data.local

import android.util.Log
import com.shade.app.data.local.entities.ChatEntity
import com.shade.app.data.local.entities.ContactEntity
import com.shade.app.data.local.entities.MessageEntity
import com.shade.app.data.local.entities.MessageStatus
import com.shade.app.data.local.entities.MessageType
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Geliştirme ve sunum amacıyla veritabanına gerçekçi sahte veri ekler.
 * Settings ekranındaki "Veri Seti Oluştur" butonuyla tetiklenir.
 */
@Singleton
class DatabaseSeeder @Inject constructor(
    private val db: ShadeDatabase
) {
    companion object {
        private const val TAG = "DatabaseSeeder"

        // Sahte kişiler: (savedName, shadeId, profileName)
        private val FAKE_CONTACTS = listOf(
            Triple("Ayşe Kaya",       "AK-1234-5678", "Ayşe"),
            Triple("Mehmet Yılmaz",   "MY-2345-6789", "Mehmet"),
            Triple("Zeynep Demir",    "ZD-3456-7890", "Zeynep"),
            Triple("Can Öztürk",      "CO-4567-8901", "Can"),
            Triple("Elif Şahin",      "ES-5678-9012", "Elif"),
            Triple("Burak Arslan",    "BA-6789-0123", "Burak"),
            Triple("Selin Çelik",     "SC-7890-1234", "Selin"),
        )

        // Gerçekçi Türkçe mesaj havuzu
        private val MESSAGES_POOL = listOf(
            "Merhaba! Nasılsın?",
            "İyiyim, sen nasılsın?",
            "Bugün buluşabilir miyiz?",
            "Tamam, saat kaçta?",
            "Öğleden sonra 3'te olur mu?",
            "Olur, nerede buluşalım?",
            "Kafede buluşalım mı?",
            "Harika fikir!",
            "Az sonra geliyorum",
            "Yolda biraz trafik var",
            "Tamam, seni bekliyorum",
            "Şu an meşgulüm, sonra yazarım",
            "Peki, görüşürüz!",
            "Dün film izledin mi?",
            "Evet izledim, çok güzeldi",
            "Hangi filmi izledin?",
            "Yeni çıkan aksiyon filmini",
            "Ben de izlemek istiyorum",
            "Kesinlikle izle, pişman olmassın",
            "Bu hafta sonu ne yapıyorsun?",
            "Henüz bir planım yok",
            "Bir yere gidelim mi?",
            "Nereye gitmek istersin?",
            "Pikniğe gidebiliriz hava güzelse",
            "Güzel olur, hava durumuna bakayım",
            "Yarın hava açık görünüyor",
            "O zaman hazırlanalım",
            "Sandviç ben yapayım",
            "Ben de içecek getiririm",
            "Süper! Sabah 10'da buluşalım",
            "Tamam görüşürüz 🎉",
            "Proje nasıl gidiyor?",
            "Yavaş ama ilerliyor",
            "Yardıma ihtiyacın var mı?",
            "Şimdilik idare ediyorum, teşekkürler",
            "Biliyorsun her zaman buradayım",
            "Çok teşekkür ederim 😊",
            "Rica ederim, başarılar!",
            "Annemi aradın mı?",
            "Evet aradım, iyiymiş",
            "İyi haber!",
            "Seni özledim",
            "Ben de seni özledim",
            "Yakında görüşürüz inşallah",
            "Evet, sabırsızlanıyorum",
            "Bu akşam müsait misin?",
            "Evet, ne var?",
            "Bir şey sormak istiyordum",
            "Söyle bakalım",
            "Yüz yüze konuşsak daha iyi olur",
            "Tamam, geliyorum 😊",
        )
    }

    suspend fun seed(myShadeId: String) {
        Log.d(TAG, "Veri seti oluşturuluyor... myShadeId=$myShadeId")

        val now = System.currentTimeMillis()
        var insertedContacts = 0
        var insertedMessages = 0

        FAKE_CONTACTS.forEach { (savedName, shadeId, profileName) ->
            val userId = UUID.randomUUID().toString()

            // Kişiyi ekle
            db.contactDao().insertContact(
                ContactEntity(
                    userId = userId,
                    shadeId = shadeId,
                    encryptionPublicKey = "fake_key_${shadeId}",
                    savedName = savedName,
                    profileName = profileName,
                    profileImagePath = null
                )
            )
            insertedContacts++

            // Chat oluştur
            val messages = generateConversation(myShadeId, shadeId, now)
            val lastMsg = messages.last()

            db.chatDao().insertOrUpdateChat(
                ChatEntity(
                    chatId = shadeId,
                    lastMessage = lastMsg.content,
                    lastMessageTimestamp = lastMsg.timestamp,
                    unreadCount = (2..5).random()
                )
            )

            // Mesajları ekle
            messages.forEach { db.messageDao().insertMessage(it) }
            insertedMessages += messages.size

            Log.d(TAG, "  → $savedName ($shadeId): ${messages.size} mesaj eklendi")
        }

        Log.d(TAG, "Tamamlandı: $insertedContacts kişi, $insertedMessages mesaj")
    }

    private fun generateConversation(
        myShadeId: String,
        otherShadeId: String,
        now: Long
    ): List<MessageEntity> {
        val count = (15..35).random()
        val messages = mutableListOf<MessageEntity>()
        // En eski mesaj ~3 gün önce, en yeni ~5 dakika önce
        val startTime = now - 3 * 24 * 60 * 60_000L
        val timeStep = (now - startTime) / count

        val pool = MESSAGES_POOL.shuffled()

        for (i in 0 until count) {
            val isMe = (i % 3 != 0) // %66 ben gönderiyorum
            val timestamp = startTime + i * timeStep + (-60_000L..60_000L).random()

            val status = if (isMe) {
                when {
                    i < count - 3 -> MessageStatus.READ
                    i < count - 1 -> MessageStatus.DELIVERED
                    else           -> MessageStatus.SENT
                }
            } else {
                MessageStatus.READ
            }

            messages.add(
                MessageEntity(
                    messageId = UUID.randomUUID().toString(),
                    senderId  = if (isMe) myShadeId else otherShadeId,
                    receiverId = if (isMe) otherShadeId else myShadeId,
                    messageType = MessageType.TEXT,
                    content   = pool[i % pool.size],
                    timestamp = timestamp,
                    status    = status
                )
            )
        }
        return messages
    }

    /** Tüm seed verisini siler (tekrar seed etmeden önce temizlemek için). */
    suspend fun clearSeedData() {
        FAKE_CONTACTS.forEach { (_, shadeId, _) ->
            db.chatDao().deleteChat(shadeId)
            // Mesajlar cascade ile silinmez, manuel sil
        }
        Log.d(TAG, "Seed verileri temizlendi")
    }
}

private fun LongRange.random(): Long = (first + (Math.random() * (last - first)).toLong())
private fun IntRange.random(): Int  = (first + (Math.random() * (last - first)).toInt())
