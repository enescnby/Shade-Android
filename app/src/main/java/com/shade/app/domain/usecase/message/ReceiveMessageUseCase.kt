package com.shade.app.domain.usecase.message

import android.util.Log
import com.shade.app.crypto.MessageCryptoManager
import com.shade.app.data.local.entities.MessageEntity
import com.shade.app.data.local.entities.MessageStatus
import com.shade.app.domain.repository.ChatRepository
import com.shade.app.domain.repository.ContactRepository
import com.shade.app.domain.repository.MessageRepository
import com.shade.app.proto.EncryptedPayload
import com.shade.app.proto.MessageType
import com.shade.app.security.KeyVaultManager
import org.bouncycastle.util.encoders.Hex
import javax.inject.Inject

class ReceiveMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
    private val chatRepository: ChatRepository,
    private val contactRepository: ContactRepository,
    private val cryptoManager: MessageCryptoManager,
    private val keyVaultManager: KeyVaultManager,
    private val sendReceiptUseCase: SendReceiptUseCase
) {

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

            val entity = MessageEntity(
                messageId = payload.messageId,
                senderId = contact.shadeId,
                receiverId = myShadeId,
                content = decryptedText,
                timestamp = payload.timestamp,
                status = MessageStatus.DELIVERED,
                messageType = when (payload.type) {
                    MessageType.TEXT -> com.shade.app.data.local.entities.MessageType.TEXT
                    MessageType.IMAGE -> com.shade.app.data.local.entities.MessageType.IMAGE
                    else -> com.shade.app.data.local.entities.MessageType.TEXT
                }
            )

            messageRepository.insertMessage(entity)
            chatRepository.updateChatWithNewMessage(contact.shadeId, decryptedText, payload.timestamp)

            sendReceiptUseCase(payload.messageId, contact.shadeId, MessageStatus.DELIVERED)
        } catch (e: Exception) {
            Log.e("ReceiveMessage", "Exception in ReceiveMessageUseCase: ${e.message}")
        }
    }
}
