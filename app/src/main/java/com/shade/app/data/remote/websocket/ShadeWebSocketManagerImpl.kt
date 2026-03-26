package com.shade.app.data.remote.websocket

import android.util.Log
import com.shade.app.proto.WebSocketMessage
import com.shade.app.security.KeyVaultManager
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShadeWebSocketManagerImpl @Inject constructor(
    private val client: OkHttpClient,
    private val keyVaultManager: KeyVaultManager
) : ShadeWebSocketManager, WebSocketListener() {

    private var webSocket: WebSocket? = null
    private val TAG = "ShadeWS"

    private val _messages = MutableSharedFlow<WebSocketMessage>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)

    override fun connect(url: String) {
        if (_connectionState.value == ConnectionState.CONNECTED) return

        val token = keyVaultManager.getAccessToken() ?: ""
        if (token.isEmpty()) {
            Log.e(TAG, "Connect iptal: Token bulunamadı!")
            return
        }

        val socketUrl = if (url.contains("?")) {
            "$url&token=$token"
        } else {
            "$url?token=$token"
        }

        Log.d(TAG, "Bağlanılıyor: $socketUrl")
        val request = Request.Builder().url(socketUrl).build()
        _connectionState.value = ConnectionState.CONNECTING
        webSocket = client.newWebSocket(request, this)
    }

    override fun disconnect() {
        Log.d(TAG, "Bağlantı kesiliyor...")
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    override fun sendMessage(message: WebSocketMessage): Boolean {
        val bytes = message.toByteArray()
        val result = webSocket?.send(ByteString.of(*bytes)) ?: false
        Log.d(TAG, "Mesaj gönderildi mi?: $result")
        return result
    }

    override fun observeMessages() = _messages.asSharedFlow()
    override fun observeConnectionState() = _connectionState.asStateFlow()

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.i(TAG, "Bağlantı AÇILDI")
        _connectionState.value = ConnectionState.CONNECTED
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        try {
            Log.d(TAG, "Yeni Binary mesaj geldi, boyutu: ${bytes.size}")
            val protoMessage = WebSocketMessage.parseFrom(bytes.toByteArray())
            val emitted = _messages.tryEmit(protoMessage)
            Log.d(TAG, "Mesaj Flow'a aktarıldı mı?: $emitted")
        } catch (e: Exception) {
            Log.e(TAG, "Mesaj parse hatası: ${e.message}")
        }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        Log.w(TAG, "Bağlantı Kapanıyor: $code / $reason")
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.e(TAG, "WebSocket HATASI: ${t.message}", t)
        _connectionState.value = ConnectionState.ERROR
    }
}
