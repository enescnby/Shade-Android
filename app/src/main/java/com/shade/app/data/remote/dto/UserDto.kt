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