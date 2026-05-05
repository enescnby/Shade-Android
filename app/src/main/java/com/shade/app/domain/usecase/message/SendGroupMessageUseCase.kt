package com.shade.app.domain.usecase.message

import android.util.Log
import com.google.protobuf.ByteString
import com.shade.app.crypto.MessageCryptoManager
import com.shade.app.data.local.entities.MessageEntity
import com.shade.app.data.local.entities.MessageStatus
import com.shade.app.data.remote.dto.GroupMemberResponse
import com.shade.app.domain.repository.ChatRepository
import com.shade.app.domain.repository.ContactRepository
import com.shade.app.domain.repository.GroupRepository
import com.shade.app.domain.repository.MessageRepository
import com.shade.app.proto.MessageType
import com.shade.app.proto.encryptedPayload
import com.shade.app.proto.webSocketMessage
import com.shade.app.security.KeyVaultManager
import org.bouncycastle.util.encoders.Hex
import java.util.UUID
import javax.inject.Inject

/**
 * Sends a text message to every member of a group using fan-out E2E encryption.
 *
 * Each member gets a separate [EncryptedPayload] message encrypted with their own
 * X25519 public key.  All payloads share the same [messageId] and carry the same
 * [groupId] field so the receiver can route the message to the correct group chat.
 */
class SendGroupMessageUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
    private val messageRepository: MessageRepository,
    private val chatRepository: ChatRepository,
    private val contactRepository: ContactRepository,
    private val cryptoManager: MessageCryptoManager,
    private val keyVaultManager: KeyVaultManager,
) {
    suspend operator fun invoke(
        groupId: String,
        groupName: String,
        text: String,
    ) {
        val myPrivKeyHex = keyVaultManager.getX25519PrivateKey() ?: return
        val myShadeId   = keyVaultManager.getShadeId()          ?: return
        val myUserId    = keyVaultManager.getUserId()            ?: return

        // Fetch current member list from the backend
        val groupResult = groupRepository.getGroup(groupId)
        if (groupResult.isFailure) {
            Log.e("SendGroupMsg", "Failed to fetch group: ${groupResult.exceptionOrNull()?.message}")
            return
        }
        val group = groupResult.getOrThrow()

        val msgId = UUID.randomUUID().toString()
        val ts    = System.currentTimeMillis()

        var atLeastOneSent = false

        for (member: GroupMemberResponse in group.members) {
            if (member.shadeId == myShadeId) continue // don't send to self

            val contact = contactRepository.getOrFetchContact(member.shadeId) ?: continue

            val sharedSecret = cryptoManager.generateSharedSecret(myPrivKeyHex, contact.encryptionPublicKey)
            val derivedKey   = cryptoManager.deriveConversationKey(sharedSecret, 1)
            val (cipherHex, nonceHex) = cryptoManager.encryptMessage(text, derivedKey)

            val socketMsg = webSocketMessage {
                payload = encryptedPayload {
                    messageId     = msgId
                    senderShadeId = myShadeId
                    senderId      = myUserId
                    receiverId    = contact.userId
                    ciphertext    = ByteString.copyFrom(Hex.decode(cipherHex))
                    this.nonce    = ByteString.copyFrom(Hex.decode(nonceHex))
                    timestamp     = ts
                    type          = MessageType.TEXT
                    groupId       = groupId
                }
            }

            val sent = messageRepository.sendWebsocketMessage(socketMsg)
            if (sent) atLeastOneSent = true
        }

        // Persist the outgoing message once (stored under group chat)
        val entity = MessageEntity(
            messageId   = msgId,
            senderId    = myShadeId,
            receiverId  = groupId,   // group chat uses groupId as the "chat ID"
            content     = text,
            timestamp   = ts,
            messageType = com.shade.app.data.local.entities.MessageType.TEXT,
            status      = if (atLeastOneSent) MessageStatus.SENT else MessageStatus.FAILED,
        )
        messageRepository.insertMessage(entity)
        chatRepository.updateLastMessage(
            chatId      = groupId,
            lastMessage = text,
            timestamp   = ts,
        )
    }
}
