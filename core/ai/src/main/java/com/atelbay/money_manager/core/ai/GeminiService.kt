package com.atelbay.money_manager.core.ai

import com.atelbay.money_manager.core.remoteconfig.ParserConfig

data class FailedAttempt(
    val config: ParserConfig,
    val error: String,
)

interface GeminiService {
    suspend fun parseContent(
        blobs: List<Pair<ByteArray, String>>,
        prompt: String,
    ): String

    suspend fun generateParserConfig(
        headerSnippet: String,
        sampleRows: String,
        existingConfigs: List<ParserConfig> = emptyList(),
        previousAttempts: List<FailedAttempt> = emptyList(),
    ): ParserConfig
}
