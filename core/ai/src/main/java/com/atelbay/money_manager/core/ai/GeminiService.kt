package com.atelbay.money_manager.core.ai

import com.atelbay.money_manager.core.remoteconfig.ParserConfig

interface GeminiService {
    suspend fun parseContent(
        blobs: List<Pair<ByteArray, String>>,
        prompt: String,
    ): String

    suspend fun generateParserConfig(sampleRows: String): ParserConfig
}
