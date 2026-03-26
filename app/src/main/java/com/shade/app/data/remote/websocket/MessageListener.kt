package com.shade.app.data.remote.websocket

import android.util.Log
import com.shade.app.BuildConfig
import com.shade.app.domain.repository.MessageRepository
import com.shade.app.domain.usecase.message.HandleIncomingReceiptUseCase
import com.shade.app.domain.usecase.message.ReceiveMessageUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageListener @Inject constructor(
    private val receiveMessageUseCase: ReceiveMessageUseCase,
    private val handleIncomingReceiptUseCase: HandleIncomingReceiptUseCase,
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
        messageRepository.observeIncomingMessages()
            .onEach { webSocketMessage ->
                when {
                    webSocketMessage.hasPayload() -> {
                        Log.d("MessageManager", "New Message")
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
}