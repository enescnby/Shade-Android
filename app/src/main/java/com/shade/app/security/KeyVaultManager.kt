package com.shade.app.security

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "shade_vault")

@Singleton
class KeyVaultManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cryptoManager: CryptoManager
) {

    private object Keys {
        val ED25519_PRIVATE_KEY = stringPreferencesKey("ED25519_PRIVATE_KEY")
        val X25519_PRIVATE_KEY = stringPreferencesKey("X25519_PRIVATE_KEY")
        val JWT_ACCESS_TOKEN = stringPreferencesKey("JWT_ACCESS_TOKEN")
        val SHADE_ID = stringPreferencesKey("SHADE_ID")
        val USER_ID = stringPreferencesKey("USER_ID") // Yeni eklendi
    }

    private fun saveValue(key: androidx.datastore.preferences.core.Preferences.Key<String>, value: String) {
        runBlocking {
            val encryptedValue = cryptoManager.encrypt(value)
            context.dataStore.edit { preferences ->
                preferences[key] = encryptedValue
            }
        }
    }

    private fun getValue(key: androidx.datastore.preferences.core.Preferences.Key<String>): String? {
        return runBlocking {
            context.dataStore.data.map { preferences ->
                preferences[key]?.let { encryptedValue ->
                    try {
                        cryptoManager.decrypt(encryptedValue)
                    } catch (e: Exception) {
                        null
                    }
                }
            }.first()
        }
    }

    fun saveEd25519PrivateKey(privateKeyHex: String) = saveValue(Keys.ED25519_PRIVATE_KEY, privateKeyHex)
    fun getEd25519PrivateKey(): String? = getValue(Keys.ED25519_PRIVATE_KEY)

    fun saveX25519PrivateKey(privateKeyHex: String) = saveValue(Keys.X25519_PRIVATE_KEY, privateKeyHex)
    fun getX25519PrivateKey(): String? = getValue(Keys.X25519_PRIVATE_KEY)

    fun saveAccessToken(token: String) = saveValue(Keys.JWT_ACCESS_TOKEN, token)
    fun getAccessToken(): String? = getValue(Keys.JWT_ACCESS_TOKEN)

    fun saveShadeId(shadeId: String) = saveValue(Keys.SHADE_ID, shadeId)
    fun getShadeId(): String? = getValue(Keys.SHADE_ID)

    fun saveUserId(userId: String) = saveValue(Keys.USER_ID, userId) // Yeni eklendi
    fun getUserId(): String? = getValue(Keys.USER_ID) // Yeni eklendi

    fun clearVault() {
        runBlocking {
            context.dataStore.edit { it.clear() }
        }
    }
}
