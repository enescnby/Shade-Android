package com.shade.app.data.remote.api

import com.shade.app.data.remote.dto.TranslationResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface TranslationService {
    @GET("get")
    suspend fun translate(
        @Query("q") text: String,
        @Query("langpair") langpair: String
    ): Response<TranslationResponse>
}
