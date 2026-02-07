package com.atelbay.money_manager.core.ai

interface GeminiService {
    suspend fun parseContent(
        blobs: List<Pair<ByteArray, String>>,
        prompt: String,
    ): String
}
