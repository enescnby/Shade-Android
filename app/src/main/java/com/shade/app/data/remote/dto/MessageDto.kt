package com.shade.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class InboxMessageDto(
    @SerializedName("message_id") val messageId: String,
    @SerializedName("sender_id") val senderId: String,
    @SerializedName("receiver_id") val receiverId: String,
    @SerializedName("ciphertext") val ciphertext: String,
    @SerializedName("nonce") val nonce: String,
    @SerializedName("message_type") val messageType: Int,
    @SerializedName("timestamp") val timestamp: Long
)

data class InboxReceiptDto(
    @SerializedName("message_id") val messageId: String,
    @SerializedName("receiver_id") val receiverId: String,
    @SerializedName("status") val status: String, // "DELIVERED" or "READ"
    @SerializedName("timestamp") val timestamp: Long
)

data class InboxResponse(
    @SerializedName("messages") val messages: List<InboxMessageDto> = emptyList(),
    @SerializedName("receipts") val receipts: List<InboxReceiptDto> = emptyList()
)

data class ReceiptRequest(
    @SerializedName("message_id") val messageId: String,
    @SerializedName("status") val status: String // "READ"
)

data class BatchReceiptRequest(
    @SerializedName("receipts") val receipts: List<ReceiptRequest>
)
