package com.atelbay.money_manager.core.ai

import com.atelbay.money_manager.core.model.TableParserProfile
import com.atelbay.money_manager.core.remoteconfig.RegexParserProfile

data class FailedAttempt(
    val config: RegexParserProfile,
    val error: String,
)

data class TableFailedAttempt(
    val config: TableParserProfile,
    val error: String,
    val failedRows: List<String> = emptyList(),
)

data class CategoryNames(
    val expense: List<String> = emptyList(),
    val income: List<String> = emptyList(),
)

data class StatementClassification(
    val statementType: String, // "text" or "table"
    val expectedTransactionCount: Int,
)

interface GeminiService {
    suspend fun classifyStatement(pdfBlob: ByteArray): StatementClassification

    suspend fun parseContent(
        blobs: List<Pair<ByteArray, String>>,
        prompt: String,
    ): String

    suspend fun generateRegexParserProfile(
        headerSnippet: String,
        sampleRows: String,
        existingConfigs: List<RegexParserProfile> = emptyList(),
        previousAttempts: List<FailedAttempt> = emptyList(),
        pdfBlob: ByteArray? = null,
        categoryNames: CategoryNames = CategoryNames(),
    ): RegexParserProfile

    suspend fun generateTableParserProfile(
        sampleTableRows: List<List<String>>,
        previousAttempts: List<TableFailedAttempt> = emptyList(),
        metadataRows: List<List<String>> = emptyList(),
        columnHeaderRow: List<String>? = null,
        categoryNames: CategoryNames = CategoryNames(),
    ): TableParserProfile
}
