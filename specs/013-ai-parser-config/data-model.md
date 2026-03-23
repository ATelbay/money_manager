# Data Model: Improve AI Parser Config Generation

**Feature Branch**: `013-ai-parser-config`
**Date**: 2026-03-22

## New Entity: FailedAttempt

**Location**: `core/ai/src/main/java/.../GeminiService.kt`

| Field | Type | Description |
|-------|------|-------------|
| `config` | `ParserConfig` | The AI-generated config that failed validation or parsing |
| `error` | `String` | Human-readable error description with specific failure category |

**Error string format** (one of):
- `"Regex syntax invalid: <exception message>"`
- `"Regex failed ReDoS safety check"`
- `"DateFormat invalid: <exception message>"`
- `"Regex timed out (possible catastrophic backtracking)"`
- `"Regex matched 0 transaction lines out of N total lines. Sample lines that should have matched:\n<line1>\n<line2>\n..."`
- `"AI generation failed: <exception message>"`

**Relationships**:
- Contains a `ParserConfig` (existing, unchanged entity)
- Used by `GeminiService.generateParserConfig()` as input (`previousAttempts` parameter)
- Created by `ParseStatementUseCase.tryRegexThenGemini()` on each failed attempt

## Modified Interface: GeminiService.generateParserConfig()

**Before**:
```
generateParserConfig(headerSnippet: String, sampleRows: String): ParserConfig
```

**After**:
```
generateParserConfig(
    headerSnippet: String,
    sampleRows: String,
    existingConfigs: List<ParserConfig> = emptyList(),
    previousAttempts: List<FailedAttempt> = emptyList(),
): ParserConfig
```

## Unchanged Entity: ParserConfig

No changes. `operationTypeMap: Map<String, String>` field type and serialization remain identical. Only the AI response schema and parser that constructs this field change — from double-decoded string to direct array-of-objects parsing.

## Schema Change: operation_type_map

**AI Response Schema** (Firebase AI SDK):
- Before: `Schema.string()` → AI returns `"{\"key\":\"val\"}"`
- After: `Schema.array(Schema.obj(key, value))` → AI returns `[{"key":"Покупка","value":"expense"}]`

**ParserConfig field**: Still `Map<String, String>` — the array is converted to a map during response parsing.
