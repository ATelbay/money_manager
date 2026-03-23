package com.atelbay.money_manager.core.ai

import com.atelbay.money_manager.core.model.TableParserConfig
import com.atelbay.money_manager.core.remoteconfig.ParserConfig

data class FailedAttempt(
    val config: ParserConfig,
    val error: String,
)

data class TableFailedAttempt(
    val config: TableParserConfig,
    val error: String,
    val failedRows: List<String> = emptyList(),
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

    suspend fun generateTableParserConfig(
        sampleTableRows: List<List<String>>,
        previousAttempts: List<TableFailedAttempt> = emptyList(),
    ): TableParserConfig
}
