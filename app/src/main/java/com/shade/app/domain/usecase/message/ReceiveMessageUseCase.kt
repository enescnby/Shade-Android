package com.shade.app.domain.usecase.message

import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.shade.app.crypto.MessageCryptoManager
import com.shade.app.data.local.entities.MessageEntity
import com.shade.app.data.local.entities.MessageStatus
import com.shade.app.domain.model.ImageMessageContent
import com.shade.app.domain.repository.ChatRepository
import com.shade.app.domain.repository.ContactRepository
import com.shade.app.domain.repository.MessageRepository
import com.shade.app.proto.EncryptedPayload
import com.shade.app.proto.MessageType
import com.shade.app.security.KeyVaultManager
import com.shade.app.util.ImageFileManager
import org.bouncycastle.util.encoders.Hex
import javax.inject.Inject

class ReceiveMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
    private val chatRepository: ChatRepository,
    private val contactRepository: ContactRepository,
    private val cryptoManager: MessageCryptoManager,
    private val keyVaultManager: KeyVaultManager,
    private val sendReceiptUseCase: SendReceiptUseCase,
    private val imageFileManager: ImageFileManager
) {
    private val gson = Gson()

    suspend operator fun invoke(payload: EncryptedPayload) {
        try {
            val contact = contactRepository.getOrFetchContact(payload.senderShadeId) ?: return
            val myPrivateKeyHex = keyVaultManager.getX25519PrivateKey() ?: return
            val myShadeId = keyVaultManager.getShadeId() ?: return

            val sharedSecret = cryptoManager.generateSharedSecret(myPrivateKeyHex, contact.encryptionPublicKey)
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
                        senderId = contact.shadeId,
                        receiverId = myShadeId,
                        content = decryptedText,
                        timestamp = payload.timestamp,
                        status = MessageStatus.DELIVERED,
                        messageType = com.shade.app.data.local.entities.MessageType.IMAGE,
                        thumbnailPath = thumbnailPath,
                        imagePath = null
                    )
                }
                else -> {
                    MessageEntity(
                        messageId = payload.messageId,
                        senderId = contact.shadeId,
                        receiverId = myShadeId,
                        content = decryptedText,
                        timestamp = payload.timestamp,
                        status = MessageStatus.DELIVERED,
                        messageType = com.shade.app.data.local.entities.MessageType.TEXT
                    )
                }
            }

            messageRepository.insertMessage(entity)

            val lastMessageText = if (payload.type == MessageType.IMAGE) "\uD83D\uDCF7 Fotoğraf" else decryptedText
            chatRepository.updateChatWithNewMessage(contact.shadeId, lastMessageText, payload.timestamp)

            sendReceiptUseCase(payload.messageId, contact.shadeId, MessageStatus.DELIVERED)
        } catch (e: Exception) {
            Log.e("ReceiveMessage", "Exception in ReceiveMessageUseCase: ${e.message}")
        }
    }
}