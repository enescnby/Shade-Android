package com.shade.app.domain.model

data class ImageMessageContent(
    val imageId: String,
    val thumbnailBase64: String,
    val imageNonceHex: String,
    val width: Int,
    val height: Int,
    val sizeBytes: Long
)