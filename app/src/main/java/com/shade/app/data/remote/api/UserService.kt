package com.shade.app.data.remote.api

import com.shade.app.data.remote.dto.LookupResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface UserService {
    @GET("user/lookup/{shadeId}")
    suspend fun lookup(
        @Header("Authorization") token: String,
        @Path("shadeId") shadeId: String
    ): Response<LookupResponse>
}