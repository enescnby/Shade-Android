package com.shade.app.domain.usecase.message

import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.shade.app.crypto.MessageCryptoManager
import com.shade.app.data.local.entities.ContactEntity
import com.shade.app.data.local.entities.MessageEntity
import com.shade.app.data.local.entities.MessageStatus
import com.shade.app.domain.model.AudioMessageContent
import com.shade.app.domain.model.ImageMessageContent
import com.shade.app.domain.repository.ChatRepository
import com.shade.app.domain.repository.ContactRepository
import com.shade.app.domain.repository.MessageRepository
import com.shade.app.proto.EncryptedPayload
import com.shade.app.proto.MessageType
import com.shade.app.security.KeyVaultManager
import com.shade.app.util.ActiveChatTracker
import com.shade.app.util.ImageFileManager
import com.shade.app.util.NotificationHelper
import org.bouncycastle.util.encoders.Hex
import javax.inject.Inject

class ReceiveMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
    private val chatRepository: ChatRepository,
    private val contactRepository: ContactRepository,
    private val cryptoManager: MessageCryptoManager,
    private val keyVaultManager: KeyVaultManager,
    private val sendReceiptUseCase: SendReceiptUseCase,
    private val imageFileManager: ImageFileManager,
    private val notificationHelper: NotificationHelper,
    private val activeChatTracker: ActiveChatTracker
) {
    private val gson = Gson()

    suspend operator fun invoke(payload: EncryptedPayload, sendReceipt: Boolean = true) {
        try {
            val myPrivateKeyHex = keyVaultManager.getX25519PrivateKey() ?: return
            val myShadeId = keyVaultManager.getShadeId() ?: return

            // Server, kullanıcının başka cihazından (örn. web) gönderdiği mesajı
            // bu cihaza echo olarak iletebiliyor. Bu durumda payload.senderShadeId
            // kendi shade_id'mize eşittir; karşı taraf payload.receiverId (user_id)
            // ile temsil edilir ve şifre çözmek için ONUN pub key'i gerekir.
            val isOutgoingEcho = payload.senderShadeId == myShadeId

            val partner: ContactEntity = if (isOutgoingEcho) {
                contactRepository.getContactByUserId(payload.receiverId) ?: run {
                    Log.w(
                        "ReceiveMessage",
                        "Self-echo received but receiver_id=${payload.receiverId} is not in local contacts; skipping ${payload.messageId}"
                    )
                    return
                }
            } else {
                contactRepository.getOrFetchContact(payload.senderShadeId) ?: return
            }

            val sharedSecret = cryptoManager.generateSharedSecret(myPrivateKeyHex, partner.encryptionPublicKey)
            val derivedKey = cryptoManager.deriveConversationKey(sharedSecret, 1)

            val decryptedText = try {
                cryptoManager.decryptMessage(
                    Hex.toHexString(payload.ciphertext.toByteArray()),
                    Hex.toHexString(payload.nonce.toByteArray()),
                    derivedKey
                )
            } catch (e: Exception) {
                Log.e("ReceiveMessage", "Decryption failed: ${e.message}")
                "Decryption Error"
            }

            // Group messages use groupId as the chat identifier
            val isGroupMessage = payload.groupId.isNotEmpty()
            val chatId = if (isGroupMessage) payload.groupId else partner.shadeId

            val entitySenderId = if (isOutgoingEcho) myShadeId else partner.shadeId
            val entityReceiverId = when {
                isGroupMessage   -> payload.groupId
                isOutgoingEcho   -> partner.shadeId
                else             -> myShadeId
            }
            val entityStatus = if (isOutgoingEcho) MessageStatus.SENT else MessageStatus.DELIVERED

            val entity = when (payload.type) {
                MessageType.IMAGE -> {
                    var thumbnailPath: String? = null
                    try {
                        val imageContent = gson.fromJson(decryptedText, ImageMessageContent::class.java)
                        val thumbnailBytes = Base64.decode(imageContent.thumbnailBase64, Base64.NO_WRAP)
                        thumbnailPath = imageFileManager.saveThumbnail(payload.messageId, thumbnailBytes)
                    } catch (e: Exception) {
                        Log.e("ReceiveMessage", "Thumbnail save failed: ${e.message}")
                    }

                    MessageEntity(
                        messageId = payload.messageId,
                        senderId = entitySenderId,
                        receiverId = entityReceiverId,
                        content = decryptedText,
                        timestamp = payload.timestamp,
                        status = entityStatus,
                        messageType = com.shade.app.data.local.entities.MessageType.IMAGE,
                        thumbnailPath = thumbnailPath,
                        imagePath = null
                    )
                }
                MessageType.AUDIO -> {
                    val durationMs = try {
                        gson.fromJson(decryptedText, AudioMessageContent::class.java).durationMs
                    } catch (e: Exception) { 0L }
                    MessageEntity(
                        messageId = payload.messageId,
                        senderId = entitySenderId,
                        receiverId = entityReceiverId,
                        content = decryptedText,
                        timestamp = payload.timestamp,
                        status = entityStatus,
                        messageType = com.shade.app.data.local.entities.MessageType.AUDIO,
                        audioDurationMs = durationMs,
                        audioPath = null  // indirilmedi henüz
                    )
                }
                else -> {
                    MessageEntity(
                        messageId = payload.messageId,
                        senderId = entitySenderId,
                        receiverId = entityReceiverId,
                        content = decryptedText,
                        timestamp = payload.timestamp,
                        status = entityStatus,
                        messageType = com.shade.app.data.local.entities.MessageType.TEXT
                    )
                }
            }

            messageRepository.insertMessage(entity)

            val photoLabel = "📷 Fotoğraf"
            val lastMessageText = when (payload.type) {
                MessageType.IMAGE -> photoLabel
                MessageType.AUDIO -> "🎤 Ses mesajı"
                else -> decryptedText
            }
            if (isOutgoingEcho) {
                // Kendi gönderdiğimiz mesaj; unread count'u kabartmıyoruz, bildirim atmıyoruz,
                // delivery receipt göndermiyoruz.
                chatRepository.updateLastMessage(chatId, lastMessageText, payload.timestamp)
            } else {
                if (activeChatTracker.activeShadeId == chatId) {
                    chatRepository.updateLastMessage(chatId, lastMessageText, payload.timestamp)
                } else {
                    chatRepository.updateChatWithNewMessage(chatId, lastMessageText, payload.timestamp)
                }

                // Group messages don't need per-message delivery receipts
                if (sendReceipt && !isGroupMessage) {
                    sendReceiptUseCase(payload.messageId, partner.shadeId, MessageStatus.DELIVERED)
                }

                if (activeChatTracker.activeShadeId != chatId) {
                    val displayName = if (isGroupMessage) payload.groupId
                                      else (partner.savedName ?: partner.shadeId)
                    val notifText = if (payload.type == MessageType.IMAGE) photoLabel else decryptedText
                    notificationHelper.showMessageNotification(displayName, notifText, chatId)
                }
            }
        } catch (e: Exception) {
            Log.e("ReceiveMessage", "Exception in ReceiveMessageUseCase: ${e.message}")
        }
    }
}
