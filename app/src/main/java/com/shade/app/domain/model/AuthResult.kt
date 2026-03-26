package com.shade.app.domain.model

data class AuthResult(
    val shadeId: String,
    val userId: String,
    val accessToken: String? = null
)