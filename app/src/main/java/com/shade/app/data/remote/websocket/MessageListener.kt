package com.shade.app.data.remote.websocket

import android.util.Log
import com.shade.app.BuildConfig
import com.shade.app.domain.repository.MessageRepository
import com.shade.app.domain.usecase.message.FetchPendingReceiptsUseCase
import com.shade.app.domain.usecase.message.HandleIncomingReceiptUseCase
import com.shade.app.domain.usecase.message.ReceiveMessageUseCase
import com.shade.app.proto.MessageAck
import com.shade.app.proto.WebSocketMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageListener @Inject constructor(
    private val receiveMessageUseCase: ReceiveMessageUseCase,
    private val handleIncomingReceiptUseCase: HandleIncomingReceiptUseCase,
    private val fetchPendingReceiptsUseCase: FetchPendingReceiptsUseCase,
    private val messageRepository: MessageRepository,
    private val webSocketManager: ShadeWebSocketManager
){
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isListening: Boolean = false

    fun startListening() {
        if (isListening) return
        isListening = true

        webSocketManager.connect(BuildConfig.WS_URL)
        Log.d("MessageManager", "Listening to WebSocket ...")

        // Fetch any pending receipts we missed while offline
        managerScope.launch {
            fetchPendingReceiptsUseCase()
        }

        messageRepository.observeIncomingMessages()
            .onEach { webSocketMessage ->
                when {
                    webSocketMessage.hasPayload() -> {
                        val messageId = webSocketMessage.payload.messageId
                        // ACK'ı HEMEN gönder - decrypt/DB yazmasını bekleme
                        val ack = WebSocketMessage.newBuilder()
                            .setAck(MessageAck.newBuilder().setMessageId(messageId).build())
                            .build()
                        webSocketManager.sendMessage(ack)

                        Log.d("MessageManager", "New Message - ACK sent for: $messageId")
                        receiveMessageUseCase(webSocketMessage.payload)
                    }
                    webSocketMessage.hasReceipt() -> {
                        Log.d("MessageManager", "New Receipt")
                        handleIncomingReceiptUseCase(webSocketMessage.receipt)
                    }
                }
            }
            .launchIn(managerScope)
    }

    fun ensureConnected(){
        if (isListening) {
            webSocketManager.connect(BuildConfig.WS_URL)

            // Fetch pending receipts on reconnect too
            managerScope.launch {
                fetchPendingReceiptsUseCase()
            }
        } else {
            startListening()
        }
    }
}
