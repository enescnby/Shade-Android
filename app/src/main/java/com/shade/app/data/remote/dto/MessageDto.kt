package com.shade.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Inbox queue item — drained from `GET /api/v1/messages/inbox`.
 *
 * Field shape mirrors the protobuf [com.shade.app.proto.EncryptedPayload]:
 *  - 1-to-1 messages: [receiverId] set, [groupId] empty.
 *  - Group messages: [groupId] set, [receiverId] empty, plus the Sender Key
 *    fields ([senderDeviceId], [senderKeyId], [chainIndex], [signature]) are
 *    populated so the recipient device can verify + ratchet locally.
 *
 * Base64 strings use the Go default `[]byte` marshalling (standard alphabet,
 * with padding).
 */
data class InboxMessageDto(
    @SerializedName("message_id")        val messageId: String,
    @SerializedName("sender_id")         val senderId: String,
    @SerializedName("receiver_id")       val receiverId: String?  = null,
    @SerializedName("group_id")          val groupId: String?     = null,
    @SerializedName("ciphertext")        val ciphertext: String,
    @SerializedName("nonce")             val nonce: String,
    @SerializedName("message_type")      val messageType: Int,
    @SerializedName("timestamp")         val timestamp: Long,

    // Group / Sender Key extensions. Empty / null for 1-to-1 messages.
    @SerializedName("sender_device_id")  val senderDeviceId: String? = null,
    @SerializedName("sender_key_id")     val senderKeyId: Long?      = null,
    @SerializedName("chain_index")       val chainIndex: Long?       = null,
    @SerializedName("signature")         val signature: String?      = null,
)

data class InboxReceiptDto(
    @SerializedName("message_id")  val messageId: String,
    @SerializedName("sender_id")   val senderId: String? = null,
    @SerializedName("receiver_id") val receiverId: String,
    @SerializedName("group_id")    val groupId: String?  = null,
    @SerializedName("status")      val status: String,    // "DELIVERED" or "READ"
    @SerializedName("timestamp")   val timestamp: Long,
)

data class InboxResponse(
    @SerializedName("messages") val messages: List<InboxMessageDto> = emptyList(),
    @SerializedName("receipts") val receipts: List<InboxReceiptDto> = emptyList(),
)

data class ReceiptRequest(
    @SerializedName("message_id") val messageId: String,
    @SerializedName("status")     val status: String, // "READ"
)

data class BatchReceiptRequest(
    @SerializedName("receipts") val receipts: List<ReceiptRequest>,
)
