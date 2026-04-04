package com.shade.app.data.remote.api

import com.shade.app.data.remote.dto.BatchReceiptRequest
import com.shade.app.data.remote.dto.PendingReceiptsResponse
import com.shade.app.data.remote.dto.UndeliveredMessageResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface MessageService {
    @GET("messages/undelivered")
    suspend fun getUndeliveredMessages(
        @Header("Authorization") token: String
    ): Response<UndeliveredMessageResponse>

    @POST("messages/receipts")
    suspend fun sendReceipts(
        @Header("Authorization") token: String,
        @Body request: BatchReceiptRequest
    ): Response<Unit>

    @GET("messages/receipts/pending")
    suspend fun getPendingReceipts(
        @Header("Authorization") token: String
    ): Response<PendingReceiptsResponse>
}