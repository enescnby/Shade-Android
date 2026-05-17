package com.shade.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LookupResponse(
    @SerializedName("user_id") val userId: String,
    @SerializedName("shade_id") val shadeId: String,
    @SerializedName("encryption_public_key") val encryptionPublicKey: String,
    @SerializedName("display_name") val displayName: String? = null   // backend desteklediğinde dolar
)

data class UpdateDisplayNameRequest(
    @SerializedName("display_name") val displayName: String
)

/**
 * Response of `GET /api/v1/keys/:id`. Returns the user's public encryption key
 * keyed off their UUID (the `/user/lookup/:shadeId` flow keys off shade_id).
 */
data class KeysResponse(
    @SerializedName("core_guard_id") val coreGuardId: String,
    @SerializedName("public_key")    val publicKey: String,
)