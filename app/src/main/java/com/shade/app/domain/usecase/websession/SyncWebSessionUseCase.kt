package com.shade.app.domain.usecase.websession

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.shade.app.crypto.WebPairingCryptoManager
import com.shade.app.data.local.dao.ChatDao
import com.shade.app.data.local.dao.MessageDao
import com.shade.app.data.remote.websocket.WebSyncSocketManager
import com.shade.app.security.KeyVaultManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Authorize → WS connect başarılı olduktan sonra çağırılır.
 *
 * Mesajlar **transfer key** ile şifrelenir (authorize handshake'inde türetilen
 * ChaCha20-Poly1305 key, HKDF info "Shade-Web-Pairing-v1"). Web tarafı aynı
 * key'e sahip olduğu için decrypt edebilir; per-contact key türetimine gerek
 * yok.
 *
 * Akış:
 *   for each chat:
 *     send { "type":"batch", "messages":[ ... ] }
 *   send { "type":"sync_complete" }
 */
class SyncWebSessionUseCase @Inject constructor(
    private val socketManager: WebSyncSocketManager,
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val keyVault: KeyVaultManager,
    private val pairingCrypto: WebPairingCryptoManager
) {
    private val gson = Gson()

    suspend operator fun invoke(): Result<Int> = runCatching {
        withContext(Dispatchers.IO) { syncBlocking() }
    }

    private suspend fun syncBlocking(): Int {
        if (!pairingCrypto.isSessionActive()) {
            error("Pairing session not active (transfer key missing)")
        }
        val myShadeId = keyVault.getShadeId() ?: error("shade_id missing")
        Log.d(TAG, "Sync started, myShadeId=$myShadeId")

        val chats = chatDao.getAllChats().first()
        Log.d(TAG, "Found ${chats.size} chats")

        var total = 0

        for (chat in chats) {
            val contactShadeId = chat.chatId
            val messages = messageDao.getMessagesForChat(contactShadeId).first()
            if (messages.isEmpty()) continue

            val items = messages.map { msg ->
                val blob = pairingCrypto.encryptWithTransferKey(
                    msg.content.toByteArray(Charsets.UTF_8)
                )
                SyncMessage(
                    messageId = msg.messageId,
                    chatId = contactShadeId,
                    senderShadeId = msg.senderId,
                    ciphertext = blob.ciphertextHex,
                    nonce = blob.nonceHex,
                    timestamp = msg.timestamp,
                    msgType = msg.messageType.name,
                    status = msg.status.name
                )
            }

            val batch = SyncBatch(type = "batch", messages = items)
            val json = gson.toJson(batch)

            val ok = socketManager.sendText(json)
            Log.d(
                TAG,
                "Batch sent: chat=$contactShadeId  count=${items.size}  bytes=${json.length}  ok=$ok"
            )
            if (!ok) error("WS send failed for chat=$contactShadeId")
            total += items.size
        }

        val complete = gson.toJson(SyncComplete(type = "sync_complete"))
        val ok = socketManager.sendText(complete)
        Log.d(TAG, "sync_complete sent  ok=$ok  totalMessages=$total")
        if (!ok) error("WS send failed for sync_complete")

        return total
    }

    private data class SyncBatch(
        val type: String,
        val messages: List<SyncMessage>
    )

    private data class SyncMessage(
        @SerializedName("message_id") val messageId: String,
        @SerializedName("chat_id") val chatId: String,
        @SerializedName("sender_shade_id") val senderShadeId: String,
        @SerializedName("ciphertext") val ciphertext: String,
        @SerializedName("nonce") val nonce: String,
        @SerializedName("timestamp") val timestamp: Long,
        @SerializedName("msg_type") val msgType: String,
        @SerializedName("status") val status: String
    )

    private data class SyncComplete(val type: String)

    private companion object {
        const val TAG = "SyncWebSession"
    }
}
