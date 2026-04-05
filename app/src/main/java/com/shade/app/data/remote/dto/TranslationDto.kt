package com.shade.app.data.remote.dto

data class TranslationResponse(
    val responseStatus: Int,
    val responseData: TranslationData
)

data class TranslationData(
    val translatedText: String
)
