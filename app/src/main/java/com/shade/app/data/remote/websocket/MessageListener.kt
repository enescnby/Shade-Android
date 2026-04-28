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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
    private var managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isListening: Boolean = false

    fun startListening() {
        if (isListening) return
        isListening = true

        webSocketManager.connect(BuildConfig.WS_URL)
        Log.d("MessageManager", "Listening to WebSocket ...")

        managerScope.launch {
            fetchPendingReceiptsUseCase()
        }

        messageRepository.observeIncomingMessages()
            .onEach { webSocketMessage ->
                when {
                    webSocketMessage.hasPayload() -> {
                        val messageId = webSocketMessage.payload.messageId
                        Log.d("MessageManager", "New Message received: $messageId")

                        // Decrypt and persist FIRST, then send ACK
                        receiveMessageUseCase(webSocketMessage.payload)

                        val ack = WebSocketMessage.newBuilder()
                            .setAck(MessageAck.newBuilder().setMessageId(messageId).build())
                            .build()
                        webSocketManager.sendMessage(ack)
                        Log.d("MessageManager", "ACK sent for: $messageId")
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

            managerScope.launch {
                fetchPendingReceiptsUseCase()
            }
        } else {
            startListening()
        }
    }

    fun stopListening() {
        isListening = false
        managerScope.cancel()
        managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
