package com.shade.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UndeliveredMessageDto(
    @SerializedName("message_id") val messageId: String,
    @SerializedName("sender_id") val senderId: String,
    @SerializedName("sender_shade_id") val senderShadeId: String,
    @SerializedName("ciphertext") val ciphertext: String,
    @SerializedName("nonce") val nonce: String,
    @SerializedName("message_type") val messageType: Int,
    @SerializedName("key_version") val keyVersion: Int,
    @SerializedName("created_at") val createdAt: String
)

data class UndeliveredMessageResponse(
    @SerializedName("messages") val messages: List<UndeliveredMessageDto>
)

data class ReceiptRequest(
    @SerializedName("message_id") val messageId: String,
    @SerializedName("status") val status: String // "READ"
)

data class BatchReceiptRequest(
    @SerializedName("receipts") val receipts: List<ReceiptRequest>
)

data class PendingReceiptDto(
    @SerializedName("message_id") val messageId: String,
    @SerializedName("status") val status: String, // "DELIVERED" or "READ"
    @SerializedName("timestamp") val timestamp: Long
)

data class PendingReceiptsResponse(
    @SerializedName("receipts") val receipts: List<PendingReceiptDto>
)