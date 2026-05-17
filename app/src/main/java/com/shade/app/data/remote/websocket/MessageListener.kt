package com.shade.app.data.remote.websocket

import android.util.Log
import com.shade.app.BuildConfig
import com.shade.app.domain.repository.MessageRepository
import com.shade.app.domain.usecase.group.HandleGroupKeyDistributionUseCase
import com.shade.app.domain.usecase.group.HandleGroupMembershipEventUseCase
import com.shade.app.domain.usecase.message.FetchInboxUseCase
import com.shade.app.domain.usecase.message.HandleIncomingReceiptUseCase
import com.shade.app.domain.usecase.message.ReceiveGroupMessageUseCase
import com.shade.app.domain.usecase.message.ReceiveMessageUseCase
import com.shade.app.proto.MessageAck
import com.shade.app.proto.WebSocketMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single subscriber on [ShadeWebSocketManager] that fans incoming
 * [WebSocketMessage]s out to the appropriate use case:
 *  - `payload` (1-to-1)  → [ReceiveMessageUseCase] → ack
 *  - `payload` (group)   → [ReceiveGroupMessageUseCase]
 *  - `receipt`           → [HandleIncomingReceiptUseCase]
 *  - `gkd` (SKDM)        → [HandleGroupKeyDistributionUseCase]
 *  - `gme`               → [HandleGroupMembershipEventUseCase]
 */
@Singleton
class MessageListener @Inject constructor(
    private val receiveMessageUseCase: ReceiveMessageUseCase,
    private val receiveGroupMessageUseCase: ReceiveGroupMessageUseCase,
    private val handleIncomingReceiptUseCase: HandleIncomingReceiptUseCase,
    private val handleGroupKeyDistribution: HandleGroupKeyDistributionUseCase,
    private val handleGroupMembershipEvent: HandleGroupMembershipEventUseCase,
    private val fetchInboxUseCase: FetchInboxUseCase,
    private val messageRepository: MessageRepository,
    private val webSocketManager: ShadeWebSocketManager,
) {
    private var managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isListening: Boolean = false

    fun startListening() {
        if (isListening) return
        isListening = true

        webSocketManager.connect(BuildConfig.WS_URL)
        Log.d(TAG, "Listening to WebSocket ...")

        managerScope.launch { fetchInboxUseCase() }

        messageRepository.observeIncomingMessages()
            .onEach { wsMsg ->
                when {
                    wsMsg.hasPayload() -> handlePayload(wsMsg)
                    wsMsg.hasReceipt() -> {
                        Log.d(TAG, "Receipt for ${wsMsg.receipt.messageId}")
                        handleIncomingReceiptUseCase(wsMsg.receipt)
                    }
                    wsMsg.hasGkd() -> {
                        Log.d(
                            TAG,
                            "SKDM in: group=${wsMsg.gkd.groupId} from=${wsMsg.gkd.senderUserId}"
                        )
                        handleGroupKeyDistribution(wsMsg.gkd)
                    }
                    wsMsg.hasGme() -> {
                        Log.d(
                            TAG,
                            "GME in: group=${wsMsg.gme.groupId} kind=${wsMsg.gme.kind} " +
                                    "subject=${wsMsg.gme.subjectId}"
                        )
                        handleGroupMembershipEvent(wsMsg.gme)
                    }
                }
            }
            .launchIn(managerScope)
    }

    private suspend fun handlePayload(wsMsg: WebSocketMessage) {
        val payload = wsMsg.payload
        val isGroup = payload.groupId.isNotEmpty()
        Log.d(TAG, "Payload in: id=${payload.messageId} group=$isGroup")

        if (isGroup) {
            // Group payloads ratchet locally — we do NOT ack the server. The
            // server's group fan-out is fire-and-forget; per-recipient
            // delivery is reported via pairwise DeliveryReceipts.
            receiveGroupMessageUseCase(payload)
        } else {
            receiveMessageUseCase(payload)
            // ACK so the server can stop re-trying. For group payloads the
            // server doesn't ACK-track per recipient.
            val ack = WebSocketMessage.newBuilder()
                .setAck(MessageAck.newBuilder().setMessageId(payload.messageId).build())
                .build()
            webSocketManager.sendMessage(ack)
            Log.d(TAG, "ACK sent for: ${payload.messageId}")
        }
    }

    fun ensureConnected() {
        if (isListening) {
            webSocketManager.connect(BuildConfig.WS_URL)
            managerScope.launch { fetchInboxUseCase() }
        } else {
            startListening()
        }
    }

    fun stopListening() {
        isListening = false
        managerScope.cancel()
        managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    private companion object {
        private const val TAG = "MessageManager"
    }
}
