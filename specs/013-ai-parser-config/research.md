# Research: Improve AI Parser Config Generation

**Feature Branch**: `013-ai-parser-config`
**Date**: 2026-03-22

## R1: operation_type_map Schema — Schema.obj() vs Schema.array()

**Decision**: Use `Schema.array(Schema.obj(...))` with explicit `key`/`value` properties.

**Rationale**: Firebase AI `Schema.obj()` requires predefined property names. Since `operation_type_map` has dynamic keys (bank-specific operation names like "Покупка", "Payment for goods and services"), we cannot use `Schema.obj()` with predefined properties. `Schema.array()` of `{key, value}` objects lets the AI return arbitrary key-value pairs.

**Alternatives considered**:
- `Schema.string()` (current) — causes double-encoding; AI returns `"{\"key\":\"value\"}"` inside JSON, fragile and error-prone.
- `Schema.obj()` with dynamic keys — Firebase AI SDK doesn't support dynamic/arbitrary keys in `Schema.obj()`.
- Free-form JSON without schema constraint for this field — would require removing `operation_type_map` from the schema entirely and parsing it separately.

**Schema change**:
```kotlin
// Before:
"operation_type_map" to Schema.string()

// After:
"operation_type_map" to Schema.array(
    Schema.obj(
        properties = mapOf(
            "key" to Schema.string(),
            "value" to Schema.enumeration(listOf("income", "expense")),
        ),
    ),
)
```

**Response parser change**:
```kotlin
// Before: double-decode from string
val raw = jsonObj["operation_type_map"]?.jsonPrimitive?.content.orEmpty()
val mapObj = json.parseToJsonElement(raw).jsonObject

// After: read array of {key, value} objects directly
val arr = jsonObj["operation_type_map"]?.jsonArray.orEmpty()
val map = arr.associate { entry ->
    val obj = entry.jsonObject
    obj["key"]!!.jsonPrimitive.content to obj["value"]!!.jsonPrimitive.content
}
```

## R2: FailedAttempt Placement

**Decision**: Define `FailedAttempt` as a top-level data class in `GeminiService.kt` (core:ai module).

**Rationale**: It's part of the `GeminiService` interface contract — `generateParserConfig()` accepts `previousAttempts: List<FailedAttempt>`. Placing it alongside the interface keeps the contract self-contained. Both core:ai and domain:import already depend on core:ai, so no new dependency edges.

**Alternatives considered**:
- In `ParseStatementUseCase.kt` — would require core:ai to depend on domain:import (violates layer direction).
- In `core:model` — overreach for a type only used between these two modules.

## R3: Few-Shot Example Selection

**Decision**: Pass all available `ParserConfig` entries to `GeminiServiceImpl`, which selects up to 3 diverse examples for the prompt.

**Rationale**: The caller (ParseStatementUseCase) already has access to both Remote Config configs and cached AI configs. Passing all of them lets GeminiServiceImpl pick the most diverse subset (different `amountFormat`, `useSignForType`, `useNamedGroups` values) to maximize the AI's understanding of the schema's capabilities.

**Selection algorithm**:
1. If ≤3 configs available, use all.
2. If >3, prioritize diversity: pick configs with different `amountFormat` values first, then different boolean flag combinations.
3. Render each selected config as a JSON example block in the prompt.

**Alternatives considered**:
- Let the caller select examples — adds complexity to ParseStatementUseCase for no benefit.
- Always use fixed hardcoded examples — stale, doesn't reflect actual working configs.

## R4: Retry Loop Design

**Decision**: Wrap steps 3-5 of `tryRegexThenGemini()` in a `repeat(MAX_RETRIES)` loop with a `MutableList<FailedAttempt>` accumulator.

**Rationale**: The retry boundary includes AI generation (step 3), validation (step 4), and regex parse (step 5). On any failure within these steps, a `FailedAttempt` is created with the specific error category and added to the list. The loop continues with the updated list passed to the next `generateParserConfig()` call.

**Error categories**:
| Failure Point | Error String Template |
|---------------|----------------------|
| `Regex(pattern)` throws | `"Regex syntax invalid: ${e.message}"` |
| `regexValidator.isReDoSSafe()` returns false | `"Regex failed ReDoS safety check"` |
| `DateTimeFormatter.ofPattern()` throws | `"DateFormat invalid: ${e.message}"` |
| `withTimeout` catches `TimeoutCancellationException` | `"Regex timed out (possible catastrophic backtracking)"` |
| Parse returns 0 transactions | `"Regex matched 0 transaction lines out of N total lines. Sample lines:\n${sampleLines}"` |
| `generateParserConfig()` throws exception | `"AI generation failed: ${e.message}"` |

**Flow**:
```
failedAttempts = mutableListOf()
repeat(3) {
    config = geminiService.generateParserConfig(header, samples, existingConfigs, failedAttempts)
    validate(config) → on failure: failedAttempts.add(FailedAttempt(config, errorString)); continue
    parse(config) → on 0 results: failedAttempts.add(...with sample lines...); continue
    → on success: cache and return
}
// All 3 failed → fall through to full-AI
```

## R5: Sample Line Count

**Decision**: Change `SAMPLE_LINE_COUNT` from 10 to 60.

**Rationale**: 10 lines often captures only headers or a partial transaction row in multi-line formats. 60 lines (~1 page) provides enough context for the AI to see repeated patterns and identify the regex structure. For short PDFs (<60 lines after header), all available lines are used naturally by `.take(60)`.

**Alternatives considered**:
- 30 lines — might still miss patterns in sparse formats.
- 100 lines — diminishing returns, increases token count/cost unnecessarily.
- Dynamic (percentage of total) — overengineered for this use case.

## R6: Prompt Language

**Decision**: Rewrite `buildParserConfigPrompt()` entirely in English.

**Rationale**: English prompts produce more consistent results across LLMs. The bank statement data itself can be in any language (Kazakh, Russian, English) — the AI analyzes the data regardless of prompt language. The existing Russian prompt was an early-development artifact.

**Content to preserve**: All structural rules (bank_id naming, named group syntax, amount_format options, operation_type_map guidance, injection guard) must be carried over to the English version. Add new sections for few-shot examples and previous failure context.
