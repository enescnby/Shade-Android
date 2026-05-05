package com.shade.app.data.remote.api

import com.shade.app.data.remote.dto.LookupResponse
import com.shade.app.data.remote.dto.UpdateDisplayNameRequest
import com.shade.app.data.remote.dto.UserStatusResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.Path

interface UserService {
    @GET("user/lookup/{shadeId}")
    suspend fun lookup(
        @Header("Authorization") token: String,
        @Path("shadeId") shadeId: String
    ): Response<LookupResponse>

    @GET("user/status/{shadeId}")
    suspend fun getUserStatus(
        @Header("Authorization") token: String,
        @Path("shadeId") shadeId: String
    ): Response<UserStatusResponse>

    /** Kendi görünen adını sunucuya günceller. Backend: PATCH /api/v1/user/displayname */
    @PATCH("user/displayname")
    suspend fun updateDisplayName(
        @Header("Authorization") token: String,
        @Body request: UpdateDisplayNameRequest
    ): Response<Unit>
}