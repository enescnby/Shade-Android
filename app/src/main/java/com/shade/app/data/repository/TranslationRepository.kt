package com.shade.app.data.repository

import android.util.Log
import com.shade.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationRepository @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "Translation"

        private val LANG_NAMES = mapOf(
            "en" to "İngilizce",
            "de" to "Almanca",
            "fr" to "Fransızca",
            "es" to "İspanyolca",
            "ar" to "Arapça",
            "ru" to "Rusça",
            "zh" to "Çince",
            "ja" to "Japonca",
            "it" to "İtalyanca",
            "pt" to "Portekizce",
            "tr" to "Türkçe"
        )
    }

    suspend fun translate(text: String, targetLang: String): String? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        return if (apiKey.isNotBlank() && apiKey != "buraya_api_keyini_yaz") {
            translateWithGemini(text, targetLang, apiKey)
        } else {
            translateWithGoogle(text, targetLang)
        }
    }

    private suspend fun translateWithGemini(
        text: String,
        targetLang: String,
        apiKey: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val targetName = LANG_NAMES[targetLang] ?: targetLang
            val prompt = "Verilen metni $targetName diline çevir. " +
                    "Metin Türkçe kısaltma veya günlük argo içerebilir (örn: 'naslsn' = 'nasılsın', 'naber' = 'ne haber'). " +
                    "Bağlamdan anlamı çıkar ve doğal bir çeviri yap. " +
                    "Sadece çeviriyi yaz, başka hiçbir şey ekleme: $text"

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                })
            }

            val body = requestJson.toString()
                .toRequestBody("application/json".toMediaType())

            val geminiClient = okhttp3.OkHttpClient.Builder()
                .connectTimeout(java.time.Duration.ofSeconds(15))
                .readTimeout(java.time.Duration.ofSeconds(30))
                .writeTimeout(java.time.Duration.ofSeconds(15))
                .build()

            val url = "https://generativelanguage.googleapis.com/v1beta/models/" +
                    "gemini-2.5-flash:generateContent?key=$apiKey"

            val request = Request.Builder().url(url).post(body).build()
            val response = geminiClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext null

            Log.d(TAG, "Gemini HTTP ${response.code}, body: $responseBody")

            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API hatası ${response.code}: $responseBody")
                return@withContext translateWithGoogle(text, targetLang)
            }

            val result = JSONObject(responseBody)
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim()

            Log.d(TAG, "Gemini çeviri: \"$text\" → \"$result\"")
            result.ifEmpty { null }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini exception: ${e.message}", e)
            translateWithGoogle(text, targetLang)
        }
    }

    private suspend fun translateWithGoogle(text: String, targetLang: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(text, "UTF-8")
                val url = "https://translate.googleapis.com/translate_a/single" +
                        "?client=gtx&sl=auto&tl=$targetLang&dt=t&q=$encoded"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0")
                    .build()
                val response = okHttpClient.newCall(request).execute()
                val body = response.body?.string() ?: return@withContext null

                val outer = JSONArray(body)
                val parts = outer.getJSONArray(0)
                val sb = StringBuilder()
                for (i in 0 until parts.length()) {
                    val part = parts.getJSONArray(i)
                    if (!part.isNull(0)) sb.append(part.getString(0))
                }
                val result = sb.toString().trim()
                Log.d(TAG, "Google çeviri: \"$text\" → \"$result\"")
                result.ifEmpty { null }
            } catch (e: Exception) {
                Log.e(TAG, "Google çeviri hatası: ${e.message}")
                null
            }
        }
}
