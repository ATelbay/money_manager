package com.atelbay.money_manager.domain.importstatement.usecase

sealed class ImportStepEvent {
    data class PdfExtracted(val lineCount: Int) : ImportStepEvent()
    data class RegexConfigAttempt(val source: String, val bankId: String? = null) : ImportStepEvent()
    data class RegexConfigResult(val source: String, val txCount: Int) : ImportStepEvent()
    data class AiConfigRequest(val attempt: Int) : ImportStepEvent()
    data class AiConfigResponse(val attempt: Int, val bankId: String) : ImportStepEvent()
    data class ValidationResult(
        val attempt: Int,
        val check: String,
        val passed: Boolean,
        val detail: String? = null,
    ) : ImportStepEvent()
    data class AiConfigParseResult(val attempt: Int, val txCount: Int) : ImportStepEvent()
    data class FullAiParse(val enabled: Boolean) : ImportStepEvent()
    data class CategoryAssignment(val count: Int) : ImportStepEvent()
    data class Deduplication(val before: Int, val after: Int) : ImportStepEvent()
    data class Complete(val txCount: Int, val method: String) : ImportStepEvent()
    data class Error(val message: String) : ImportStepEvent()
    data class TableExtracted(val rowCount: Int, val columnCount: Int) : ImportStepEvent()
    data class TableConfigAttempt(val source: String, val bankId: String? = null) : ImportStepEvent()
    data class TableConfigResult(val source: String, val txCount: Int) : ImportStepEvent()
    data class AiTableConfigRequest(val attempt: Int) : ImportStepEvent()
    data class AiTableConfigResponse(val attempt: Int, val bankId: String) : ImportStepEvent()
    data class AiTableConfigParseResult(val attempt: Int, val txCount: Int) : ImportStepEvent()
    data class Classification(val statementType: String?, val expectedCount: Int) : ImportStepEvent()
}
