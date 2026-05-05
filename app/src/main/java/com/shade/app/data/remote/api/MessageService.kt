package com.shade.app.data.remote.api

import com.shade.app.data.remote.dto.BatchReceiptRequest
import com.shade.app.data.remote.dto.InboxResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface MessageService {
    /**
     * Pulls all queued messages and receipts for the current user and clears the queue server-side.
     * @param limit 1-500, default 100.
     */
    @GET("messages/inbox")
    suspend fun getInbox(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int = 100
    ): Response<InboxResponse>

    @POST("messages/receipts")
    suspend fun sendReceipts(
        @Header("Authorization") token: String,
        @Body request: BatchReceiptRequest
    ): Response<Unit>
}
