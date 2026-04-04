package com.shade.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    @SerializedName("identity_public_key") val identityPublicKey: String,
    @SerializedName("encrypted_identity_private_key") val encryptedIdentityPrivateKey: String,
    @SerializedName("encryption_public_key") val encryptionPublicKey: String,
    @SerializedName("encrypted_encryption_private_key") val encryptedEncryptionPrivateKey: String,
    @SerializedName("salt") val salt: String,
    @SerializedName("device_model") val deviceModel: String,
    @SerializedName("fcm_token") val fcmToken: String
)

data class RegisterResponse(
    @SerializedName("core_guard_id") val shadeId: String,
    @SerializedName("user_id") val userId: String?,
    @SerializedName("message") val message: String
)

data class LoginInitRequest(
    @SerializedName("core_guard_id") val shadeId: String,
)

data class LoginInitResponse(
    @SerializedName("encrypted_identity_private_key") val encryptedIdentityPrivateKey: String,
    @SerializedName("encrypted_encryption_private_key") val encryptedEncryptionPrivateKey: String,
    @SerializedName("salt") val salt: String,
    @SerializedName("challenge") val challenge: String
)

data class LoginVerifyRequest(
    @SerializedName("core_guard_id") val shadeId: String,
    @SerializedName("challenge") val challenge: String,
    @SerializedName("signature") val signature: String,
    @SerializedName("device_model") val deviceModel: String,
    @SerializedName("fcm_token") val fcmToken: String
)

data class LoginVerifyResponse(
    @SerializedName("core_guard_id") val shadeId: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("message") val message: String
)
