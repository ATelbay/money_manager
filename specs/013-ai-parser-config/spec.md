# Feature Specification: Improve AI Parser Config Generation

**Feature Branch**: `013-ai-parser-config`
**Created**: 2026-03-22
**Status**: Draft
**Input**: User description: "Improve the Gemini-based generateParserConfig flow — add few-shot examples, retry logic with failure tracing, more sample data, fix operation_type_map double-encoding, and switch prompt to English"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - AI generates a working regex config with few-shot examples and more data (Priority: P1)

A user imports a bank statement PDF from an unsupported bank. The system extracts text, finds no matching Remote Config or cached regex, and calls Gemini to generate a ParserConfig. The AI receives ~60 lines of sample data (roughly a full page) plus 2-3 working config examples as few-shot references, producing a regex that successfully parses the statement.

**Why this priority**: This is the core value — making AI-generated configs actually work. Currently they fail because the prompt lacks examples and sends only 10 lines of data.

**Independent Test**: Import a PDF from a bank not in Remote Config. Verify the AI generates a config that parses at least some transactions, rather than falling through to full-AI parsing.

**Acceptance Scenarios**:

1. **Given** a PDF from an unknown bank with tabular transaction data, **When** the system calls Gemini with 60 sample lines and 2-3 existing config examples, **Then** Gemini returns a ParserConfig whose regex matches at least 1 transaction line.
2. **Given** no existing configs are available (first launch, empty Remote Config), **When** the system calls Gemini, **Then** the prompt still works without a few-shot examples section.

---

### User Story 2 - AI retries with failure tracing on bad configs (Priority: P1)

When the first AI-generated config fails validation (bad regex syntax, ReDoS, invalid date format, zero matches, or timeout), the system retries up to 3 times total. Each retry sends the previous failed configs and specific error descriptions to Gemini so it can correct its mistakes.

**Why this priority**: Without retries, a single bad generation means immediate fallback to expensive full-AI parsing. Retries with error context dramatically increase success rate.

**Independent Test**: Trigger a scenario where the first AI config fails validation. Verify the system retries with the failure context and that the retry prompt includes the failed config and error string.

**Acceptance Scenarios**:

1. **Given** Gemini returns a config with invalid regex syntax, **When** the system retries, **Then** the retry request includes the failed ParserConfig and error "Regex syntax invalid: [details]".
2. **Given** Gemini returns a config that matches 0 lines, **When** the system retries, **Then** the retry request includes 3-5 sample lines from the PDF that should have matched.
3. **Given** Gemini returns a config that times out during parsing (catastrophic backtracking), **When** the system retries, **Then** the retry request includes error "Regex timed out (possible catastrophic backtracking)".
4. **Given** Gemini returns a config that fails ReDoS safety check, **When** the system retries, **Then** the retry request includes error "Regex failed ReDoS safety check".
5. **Given** all 3 attempts fail, **When** retries are exhausted, **Then** the system proceeds to full-AI parsing with the same behavior as today.
6. **Given** a network error occurs during a retry attempt, **When** the error is caught, **Then** it counts as a failed attempt and the loop continues.

---

### User Story 3 - operation_type_map returned as proper JSON object (Priority: P2)

The AI returns `operation_type_map` as a native JSON object instead of a JSON-encoded string inside JSON. The system also instructs the AI to prefer sign-based type detection over operation_type_map when the statement uses +/- signs.

**Why this priority**: Double-encoded JSON is fragile and causes silent parsing failures. Fixing the schema eliminates an entire class of errors.

**Independent Test**: Call Gemini and verify the response contains `operation_type_map` as a JSON object. Verify the response parser reads it directly without double-decoding.

**Acceptance Scenarios**:

1. **Given** Gemini generates a config for a bank with named operation types, **When** the response is parsed, **Then** `operation_type_map` is read as a JSON object directly, not decoded from a string.
2. **Given** a bank statement that uses +/- signs for income/expense, **When** the AI generates a config, **Then** it sets `use_sign_for_type=true` or `negative_sign_means_expense=true` instead of populating `operation_type_map`.

---

### User Story 4 - Prompt rewritten in English (Priority: P3)

The Gemini prompt is rewritten in English. Bank statement data can be in Kazakh, Russian, or English — the prompt language is independent of the data language. English prompts produce more reliable and consistent results across LLMs.

**Why this priority**: Improves maintainability and cross-model compatibility. Lower priority because the Russian prompt technically functions.

**Independent Test**: Read the prompt text and verify it is entirely in English. Verify that parsing still works for Russian-language and Kazakh-language bank statements.

**Acceptance Scenarios**:

1. **Given** the AI prompt builder, **When** it constructs the prompt, **Then** all instructional text is in English.
2. **Given** a bank statement in Russian or Kazakh, **When** the English prompt is used, **Then** the AI still correctly identifies transaction patterns in the non-English text.

---

### Edge Cases

- What happens when the PDF has fewer than 60 non-blank lines? → Use all available lines; no padding or error needed.
- What happens when Gemini API is unavailable on all 3 retries? → Fall through to full-AI parsing or return error (existing behavior preserved).
- What happens when there are no existing configs to pass as few-shot examples? → Send prompt without the examples section; the prompt must work without few-shot examples.
- What happens when a retry itself throws a network exception? → Count it as a failed attempt, continue to next retry or fall through.
- What happens when the generated regex is valid but matches garbage data? → Not addressed in this feature; existing downstream validation handles this.
- What happens when all existing configs are from the same bank? → Still include them; diverse examples are preferred but not required.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST send at least 60 non-blank lines of PDF text as sample data to the AI (increased from 10).
- **FR-002**: System MUST include 2-3 existing working ParserConfig entries as few-shot JSON examples in the AI prompt.
- **FR-003**: System MUST collect existing configs from both Remote Config and cached AI configs and pass them to the AI service.
- **FR-004**: System MUST retry AI config generation up to 3 total attempts before falling through to full-AI parsing.
- **FR-005**: System MUST track each failed attempt with a data structure containing the failed ParserConfig and a specific error description string.
- **FR-006**: System MUST classify failures into these specific categories: invalid regex syntax, ReDoS failure, invalid date format, regex timeout, and zero matches.
- **FR-007**: System MUST include 3-5 sample transaction lines from the PDF in the error description when the generated regex matches 0 lines.
- **FR-008**: System MUST pass all previous failed attempts to subsequent AI calls so the AI can learn from its mistakes.
- **FR-009**: System MUST define `operation_type_map` in the AI response schema as a structured object, not a string.
- **FR-010**: System MUST parse `operation_type_map` from the AI response as a JSON object directly, without double-decoding from a string.
- **FR-011**: System MUST instruct the AI to prefer `use_sign_for_type` or `negative_sign_means_expense` over `operation_type_map` when the statement uses +/- signs.
- **FR-012**: System MUST write all AI prompt instructions in English.
- **FR-013**: System MUST NOT modify the ParserConfig data class or its serialization format.
- **FR-014**: System MUST NOT modify RegexStatementParser behavior.
- **FR-015**: System MUST NOT modify the full-parse path (parseContent / parseWithGemini).

### Key Entities

- **FailedAttempt**: Represents a failed AI config generation attempt. Contains the generated ParserConfig that failed and a human-readable error description string explaining why it failed.
- **ParserConfig** (existing, unchanged): The regex-based parser configuration. Contains bank_id, transaction_pattern, date_format, operation_type_map, and various boolean flags.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: AI-generated regex configs successfully parse at least 1 transaction on first-or-retry attempt for bank statements with clear tabular data, reducing fallback to full-AI parsing.
- **SC-002**: The retry mechanism provides specific, actionable error descriptions to the AI on each subsequent attempt, enabling self-correction.
- **SC-003**: `operation_type_map` is correctly parsed from AI responses without double-encoding errors in 100% of cases.
- **SC-004**: The AI prompt is entirely in English while correctly handling bank statements in Kazakh, Russian, and English.
- **SC-005**: No regressions — existing Remote Config and cached AI config parsing paths continue to work identically.

## Assumptions

- The Gemini model (2.5 Flash) can effectively use few-shot examples to produce better regex patterns.
- 60 lines of sample data is sufficient to capture a representative set of transaction rows (~1 page).
- 3 retry attempts is a reasonable balance between success rate and latency/cost.
- The existing 5-second timeout per regex parse attempt is sufficient and does not need adjustment.
- Network errors during retries count as failed attempts (not infinite-retry).

## Constraints

- ParserConfig data class and its serialization are frozen (shared with Remote Config).
- RegexStatementParser is not modified.
- The full-parse path (parseContent / parseWithGemini) is not modified.
- Only the regex generation path in the import use case is affected.

## Scope

**In scope:**
- Increasing sample data sent to AI (10 → 60 lines)
- Adding few-shot examples from existing configs to AI prompt
- Adding retry loop (up to 3 attempts) with failure tracing
- Fixing operation_type_map schema from string to structured object
- Better "0 matched" diagnostics with sample lines
- Rewriting prompt in English
- Adding FailedAttempt data structure

**Out of scope:**
- Changing ParserConfig data class or serialization
- Modifying RegexStatementParser
- Modifying full-AI parse path (parseWithGemini)
- Adding new bank support
- UI changes
- Cloud Function changes
