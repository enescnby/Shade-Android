package com.shade.app.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class KeyVaultManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "shade_secret_vault",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveEd25519PrivateKey(privateKeyHex: String) {
        sharedPreferences.edit().putString("ED25519_PRIVATE_KEY", privateKeyHex).apply()
    }

    fun getEd25519PrivateKey(): String? {
        return sharedPreferences.getString("ED25519_PRIVATE_KEY", null)
    }

    fun saveX25519PrivateKey(privateKeyHex: String) {
        sharedPreferences.edit().putString("X25519_PRIVATE_KEY", privateKeyHex).apply()
    }

    fun getX25519PrivateKey(): String? {
        return sharedPreferences.getString("X25519_PRIVATE_KEY", null)
    }

    fun saveAccessToken(token: String) {
        sharedPreferences.edit().putString("JWT_ACCESS_TOKEN", token).apply()
    }

    fun getAccessToken(): String? {
        return sharedPreferences.getString("JWT_ACCESS_TOKEN", null)
    }

    fun clearVault() {
        sharedPreferences.edit().clear().apply()
    }
}