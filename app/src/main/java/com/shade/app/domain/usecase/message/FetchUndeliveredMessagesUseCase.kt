package com.shade.app.domain.usecase.message

import android.util.Base64
import android.util.Log
import com.google.protobuf.ByteString
import com.shade.app.data.remote.api.MessageService
import com.shade.app.data.remote.dto.UndeliveredMessageDto
import com.shade.app.proto.EncryptedPayload
import com.shade.app.proto.MessageType
import com.shade.app.security.KeyVaultManager
import java.time.Instant
import javax.inject.Inject

class FetchUndeliveredMessagesUseCase @Inject constructor(
    private val messageService: MessageService,
    private val keyVaultManager: KeyVaultManager,
    private val receiveMessageUseCase: ReceiveMessageUseCase
){
    suspend operator fun invoke() {
        try {
            val token = keyVaultManager.getAccessToken() ?: return
            val response = messageService.getUndeliveredMessages("Bearer $token")

            if (!response.isSuccessful) {
                Log.e("FetchUndelivered", "HTTP ${response.code()}: ${response.message()}")
                return
            }

            val messages = response.body()?.messages ?: return
            Log.d("FetchUndelivered", "${messages.size} undelivered message(s) found")

            for (dto in messages) {
                try {
                    val payload = dto.toEncryptedPayload()
                    receiveMessageUseCase(payload, sendReceipt = false)
                } catch (e: Exception) {
                    Log.e("FetchUndelivered", "Failed to process message ${dto.messageId}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("FetchUndelivered", "Failed to fetch undelivered messages: ${e.message}")
        }
    }

    private fun UndeliveredMessageDto.toEncryptedPayload(): EncryptedPayload {
        return EncryptedPayload.newBuilder()
            .setMessageId(messageId)
            .setSenderId(senderId)
            .setSenderShadeId(senderShadeId)
            .setCiphertext(ByteString.copyFrom(Base64.decode(ciphertext, Base64.DEFAULT)))
            .setNonce(ByteString.copyFrom(Base64.decode(nonce, Base64.DEFAULT)))
            .setTimestamp(java.time.OffsetDateTime.parse(createdAt).toInstant().toEpochMilli())
            .setType(if (messageType == 1) MessageType.IMAGE else MessageType.TEXT)
            .build()
    }
}