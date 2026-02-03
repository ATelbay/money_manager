package com.atelbay.money_manager.core.ai

interface GeminiService {
    suspend fun parseContent(
        imageBytes: List<ByteArray>,
        prompt: String,
    ): String
}
