package com.shade.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.chatPrefsDataStore by preferencesDataStore(name = "chat_prefs")

@Singleton
class ChatPrefsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val BG_PREFIX   = "bg_"
        private const val AD_PREFIX   = "ad_"
    }

    // ── Background color (stored as ARGB Int) ──────────────────────────────────
    fun getChatBackground(chatId: String): Flow<Int?> {
        val key = intPreferencesKey(BG_PREFIX + chatId)
        return context.chatPrefsDataStore.data.map { it[key] }
    }

    suspend fun setChatBackground(chatId: String, colorArgb: Int?) {
        val key = intPreferencesKey(BG_PREFIX + chatId)
        context.chatPrefsDataStore.edit { prefs ->
            if (colorArgb != null) prefs[key] = colorArgb else prefs.remove(key)
        }
    }

    // ── Auto-delete timer (minutes; 0 = disabled) ──────────────────────────────
    fun getAutoDeleteMinutes(chatId: String): Flow<Int> {
        val key = intPreferencesKey(AD_PREFIX + chatId)
        return context.chatPrefsDataStore.data.map { it[key] ?: 0 }
    }

    suspend fun setAutoDeleteMinutes(chatId: String, minutes: Int) {
        val key = intPreferencesKey(AD_PREFIX + chatId)
        context.chatPrefsDataStore.edit { it[key] = minutes }
    }
}
