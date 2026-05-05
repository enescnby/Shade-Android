package com.shade.app.domain.model

data class AudioMessageContent(
    val audioId: String,
    val audioNonceHex: String,
    val durationMs: Long,
    val sizeBytes: Long
)
