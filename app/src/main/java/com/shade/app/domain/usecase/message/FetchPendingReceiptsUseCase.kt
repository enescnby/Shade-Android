package com.shade.app.domain.usecase.message

import android.util.Log
import com.shade.app.data.local.entities.MessageStatus
import com.shade.app.data.remote.api.MessageService
import com.shade.app.domain.repository.MessageRepository
import com.shade.app.security.KeyVaultManager
import javax.inject.Inject

class FetchPendingReceiptsUseCase @Inject constructor(
    private val messageService: MessageService,
    private val messageRepository: MessageRepository,
    private val keyVaultManager: KeyVaultManager
) {
    suspend operator fun invoke() {
        try {
            val token = keyVaultManager.getAccessToken() ?: return
            val response = messageService.getPendingReceipts("Bearer $token")

            if (!response.isSuccessful) {
                Log.e("FetchReceipts", "HTTP ${response.code()}: ${response.message()}")
                return
            }

            val receipts = response.body()?.receipts ?: return
            Log.d("FetchReceipts", "${receipts.size} pending receipt(s) found")

            for (receipt in receipts) {
                val status = when (receipt.status) {
                    "DELIVERED" -> MessageStatus.DELIVERED
                    "READ" -> MessageStatus.READ
                    else -> continue
                }
                messageRepository.updateMessageStatusIfForward(receipt.messageId, status)
            }
        } catch (e: Exception) {
            Log.e("FetchReceipts", "Failed to fetch pending receipts: ${e.message}")
        }
    }
}
