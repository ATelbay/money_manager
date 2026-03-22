# Quickstart: Improve AI Parser Config Generation

**Feature Branch**: `013-ai-parser-config`
**Date**: 2026-03-22

## Overview

This feature modifies 4 existing files across 3 modules (core:ai, core:parser, domain:import). No new modules, no new Gradle dependencies, no UI changes.

## Files to Modify

### 1. `core/ai/.../GeminiService.kt` (interface)
- Add `data class FailedAttempt(val config: ParserConfig, val error: String)`
- Update `generateParserConfig()` signature: add `existingConfigs: List<ParserConfig>` and `previousAttempts: List<FailedAttempt>` parameters

### 2. `core/ai/.../GeminiServiceImpl.kt` (implementation)
- **Schema**: Replace `"operation_type_map" to Schema.string()` with `Schema.array(Schema.obj(key, value))`
- **Prompt**: Rewrite `buildParserConfigPrompt()` in English with sections for:
  - Few-shot examples (rendered from `existingConfigs`, up to 3)
  - Previous failed attempts (rendered from `previousAttempts`)
  - Preference for `use_sign_for_type`/`negative_sign_means_expense` over `operation_type_map`
- **Parser**: Update `parseParserConfigResponse()` to read `operation_type_map` as JSON array of `{key, value}` objects instead of double-decoding a string

### 3. `core/parser/.../StatementParser.kt`
- Change `SAMPLE_LINE_COUNT` from `10` to `60`

### 4. `domain/import/.../ParseStatementUseCase.kt`
- Wrap steps 3-5 of `tryRegexThenGemini()` in a `repeat(3)` loop
- Collect `FailedAttempt` on each failure with specific error category
- Pass `failedAttempts` list and existing configs to `generateParserConfig()`
- Extract 3-5 sample lines for "0 matched" diagnostics

## Key Implementation Details

### Retry Loop Structure (ParseStatementUseCase)
```
val failedAttempts = mutableListOf<FailedAttempt>()
val allConfigs = parserConfigProvider.getConfigs() + loadCachedAiConfigs()

repeat(MAX_AI_RETRIES) { attempt ->
    try {
        val config = geminiService.generateParserConfig(
            headerSnippet, sampleRows, allConfigs, failedAttempts
        )
        // Validate: ReDoS, regex syntax, date format
        // Parse with timeout
        // On success: cache and return
        // On validation/parse failure: add to failedAttempts, continue
    } catch (e: Exception) {
        // Network/API error: add to failedAttempts with null config, continue
    }
}
// Fall through to full-AI parsing
```

### Few-Shot Example Selection (GeminiServiceImpl)
Pick up to 3 configs with diverse `amountFormat`/boolean flag combinations. Render as JSON blocks in the prompt.

### Error Diagnostics for 0-Match Case
When regex matches 0 lines, extract sample lines from the PDF text:
```kotlin
val sampleLines = pdfText.lines()
    .drop(HEADER_LINE_COUNT)
    .filter { it.isNotBlank() }
    .take(5)
    .joinToString("\n")
val error = "Regex matched 0 transaction lines out of $totalLines total lines. " +
    "Sample lines that should have matched:\n$sampleLines"
```

## Testing Strategy

- **GeminiServiceImpl**: Test prompt construction with/without few-shot examples and failed attempts. Test operation_type_map parsing from array format.
- **ParseStatementUseCase**: Test retry loop exhaustion, successful retry on 2nd/3rd attempt, error category classification.
- **StatementParser**: Verify `extractSampleRows()` returns up to 60 lines.

## Build & Verify

```bash
./gradlew :core:ai:test :core:parser:test :domain:import:test
./gradlew assembleDebug
```
