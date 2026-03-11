# Feature Specification: AI-Driven Parser Auto-Update

**Feature Branch**: `004-ai-regex-autoupdate`
**Created**: 2026-03-11
**Status**: Draft
**Input**: User description: "AI-driven auto-update системы парсеров банковских выписок"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Import Statement from Unsupported Bank (Priority: P1)

A user selects a PDF bank statement from a bank not yet supported (no matching bank markers in any existing parser configuration). Instead of falling back to expensive per-transaction AI parsing, the system sends a small sample (5-10 table rows) to Gemini AI and asks it to generate a complete parser configuration. The system then uses that generated configuration to parse the entire PDF locally with the existing regex engine, showing the user a preview of parsed transactions. The user reviews and imports them as usual.

**Why this priority**: This is the core value proposition — enabling any bank's statement to be parsed quickly and cheaply without a full-AI fallback, reusing the proven regex engine.

**Independent Test**: Can be fully tested by importing a PDF from an unsupported bank and verifying that transactions are correctly parsed and previewed using the AI-generated configuration.

**Acceptance Scenarios**:

1. **Given** a user selects a PDF from an unsupported bank, **When** the system cannot detect the bank via existing markers, **Then** the system extracts 5-10 sample rows from the PDF and sends them to Gemini AI to generate a parser configuration.
2. **Given** Gemini returns a valid parser configuration, **When** the system applies it to the full PDF, **Then** the user sees a preview with correctly parsed transactions (date, amount, type, details).
3. **Given** Gemini returns a configuration that fails to parse any transactions, **When** the regex engine returns 0 results, **Then** the system falls back to the existing full-AI parsing mode and informs the user.
4. **Given** a user is offline or Gemini is unavailable, **When** the config generation request fails, **Then** the system shows an error message and does not crash.

---

### User Story 2 - Crowdsourced Config Validation and Sharing (Priority: P2)

After a user successfully imports transactions using an AI-generated parser configuration, the system anonymizes a sample of the statement data (replacing merchant names, removing account numbers) and submits the validated configuration along with the anonymized sample to a shared candidate pool. When multiple independent users successfully import from the same bank, the configuration gains confidence through incrementing a success counter.

**Why this priority**: This enables the crowdsourcing loop — each successful import contributes to the community knowledge base, but the feature delivers value even without this step (P1 works standalone).

**Independent Test**: Can be tested by importing from an unsupported bank, confirming the import, and verifying that a candidate record appears in the shared pool with the correct anonymized data.

**Acceptance Scenarios**:

1. **Given** a user successfully imports transactions using an AI-generated config, **When** the user confirms the import, **Then** the system anonymizes the sample and submits a candidate record containing bankId, parserConfig, anonymizedSample, userId_hash, successCount (initially 1), and createdAt.
2. **Given** a candidate record already exists for the same bank with a matching configuration, **When** another user successfully imports from the same bank, **Then** the existing candidate's successCount is incremented rather than creating a duplicate.
3. **Given** two different configurations exist for the same bank, **When** both receive successful imports, **Then** each configuration tracks its own successCount independently.
4. **Given** the anonymization process runs on a sample, **When** the anonymized data is produced, **Then** all merchant names are replaced with sequential placeholders ("MERCHANT_1", "MERCHANT_2"), all account/card numbers are removed, and amounts and dates are preserved.

---

### User Story 3 - Automatic Config Promotion (Priority: P3)

When a candidate configuration reaches a configurable success threshold (default: 3 independent users), it is automatically promoted to the official parser configuration list. After promotion, all users receive the new bank support without any app update — the next configuration refresh delivers the new parser.

**Why this priority**: This is the automation capstone that closes the loop, but requires P1 and P2 to function. The system delivers value even with manual promotion.

**Independent Test**: Can be tested by simulating the threshold being reached and verifying the configuration appears for all clients on their next refresh.

**Acceptance Scenarios**:

1. **Given** a candidate configuration has successCount equal to the promotion threshold, **When** the threshold is reached, **Then** the configuration is automatically added to the official parser configuration list.
2. **Given** multiple candidate configurations exist for the same bank (e.g., different statement variants), **When** each candidate independently reaches the success threshold, **Then** each is promoted separately, and the system tries all promoted configs for that bank sequentially at parse time.
3. **Given** a configuration has been promoted, **When** any user opens the app and the configuration refresh interval elapses, **Then** the new bank appears in the list of supported banks and statements from that bank are parsed via regex without AI involvement.
4. **Given** a promoted configuration conflicts with an existing manually-configured bank, **When** promotion is attempted, **Then** the promotion is skipped and the conflict is logged for manual review.

---

### User Story 4 - ReDoS Safety Validation (Priority: P2)

Before any AI-generated regex is stored or used, the system validates it against catastrophic backtracking patterns. Regex patterns that could cause exponential execution time are rejected, preventing denial-of-service conditions on user devices.

**Why this priority**: Security-critical — without this, a poorly-generated regex could freeze the app. Equal priority with P2 since it gates the crowdsourcing pipeline.

**Independent Test**: Can be tested by submitting known-vulnerable regex patterns and verifying they are rejected before storage.

**Acceptance Scenarios**:

1. **Given** Gemini generates a regex pattern, **When** the pattern contains nested quantifiers (e.g., `(a+)+`), **Then** the system rejects the configuration and falls back to full-AI parsing.
2. **Given** a valid regex pattern is generated, **When** it passes ReDoS validation, **Then** it proceeds through the normal parsing flow.
3. **Given** a candidate is submitted to the shared pool, **When** the regex fails ReDoS validation, **Then** the candidate is not stored and the user is not affected (import still succeeds via full-AI fallback).

---

### Edge Cases

- What happens when the PDF has no recognizable table structure (e.g., scanned image-only PDF)? The system should detect zero parsed rows from the sample extraction and fall back to full-AI parsing.
- What happens when Gemini generates a syntactically invalid regex? The system should catch the parse error, reject the config, and fall back to full-AI parsing.
- What happens when two users submit conflicting configs for the same bank simultaneously? Each config is stored as a separate candidate; the one with the higher successCount wins at promotion time.
- What happens when the promotion threshold is changed after candidates already exist? Existing candidates are re-evaluated against the new threshold on the next successful import event.
- What happens when the user's device is offline during candidate submission? The submission is silently skipped; the user's local import is not affected.
- What happens when a promoted config turns out to be incorrect for edge-case statements? Out of scope for this iteration; the config can be manually overridden in the official configuration.

## Clarifications

### Session 2026-03-11

- Q: How should bankId be normalized to prevent fragmentation in the candidate pool? → A: AI prompt includes normalization rules (lowercase latin slug, e.g., "halyk_bank") plus known bank name mappings for existing banks.
- Q: How should candidate configs be matched for deduplication (FR-010)? → A: Match by bankId + transactionPattern only; auxiliary fields (dateFormat, operationTypeMap, etc.) are ignored for matching purposes.
- Q: How to handle different statement variants (e.g., credit vs debit) from the same bank? → A: Store as separate candidates per regex; a bank can have multiple promoted configs, tried sequentially at parse time.
- Q: How should Gemini return the generated ParserConfig — free-form JSON or structured output? → A: Use structured output with a declared response schema matching ParserConfig fields, guaranteeing valid JSON shape.
- Q: Where should AI-generated configs be cached locally (FR-013)? → A: DataStore — serialized list of AI-generated ParserConfigs keyed by bankId. Remote Config configs remain separate (fetched/cached in-memory as before). Local cache is for interim configs not yet promoted.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST extract a sample of 5-10 table rows from a PDF when no existing parser configuration matches the bank.
- **FR-002**: System MUST send the extracted sample to Gemini AI with a structured prompt requesting a parser configuration compatible with the existing ParserConfig schema (bankId, transactionPattern, dateFormat, amountFormat, operationTypeMap, skipPatterns, joinLines, useSignForType, negativeSignMeansExpense, useNamedGroups, deduplicateMaxAmount).
- **FR-003**: System MUST use Gemini's structured output mode with a declared response schema matching the ParserConfig fields, guaranteeing a valid JSON shape. The system MUST still validate semantic correctness (e.g., regex syntax, dateFormat validity) after receiving the response.
- **FR-004**: System MUST attempt to parse the full PDF using the AI-generated configuration via the existing regex engine before falling back to full-AI parsing.
- **FR-005**: System MUST validate all AI-generated regex patterns against ReDoS vulnerabilities (nested quantifiers, overlapping alternations) before use or storage.
- **FR-006**: System MUST identify the bank from the PDF header/content via AI and normalize the bankId to a lowercase latin slug (e.g., "halyk_bank", "kaspi"). The AI prompt MUST include a mapping of known bank names to canonical slugs and a slug generation rule (lowercase, latin transliteration, underscores) for unknown banks.
- **FR-007**: System MUST anonymize statement samples before submission: replace merchant/recipient names with sequential placeholders ("MERCHANT_1", "MERCHANT_2", etc.), remove account/card numbers, preserve amounts and dates.
- **FR-008**: System MUST hash the user identifier before including it in candidate submissions.
- **FR-009**: System MUST submit validated configurations to a shared candidate pool containing: bankId, parserConfig, anonymizedSample, userId_hash, successCount, and createdAt.
- **FR-010**: System MUST increment successCount on an existing candidate when another user successfully imports using a configuration with the same bankId and transactionPattern, rather than creating a duplicate. Auxiliary fields (dateFormat, operationTypeMap, skipPatterns, etc.) are not considered for matching.
- **FR-011**: System MUST automatically promote a candidate configuration to the official list when its successCount reaches the configurable threshold (default: 3).
- **FR-012**: System MUST promote all candidates that reach the success threshold independently, even if multiple candidates exist for the same bankId (e.g., different statement variants like credit vs debit card). At parse time, the system tries each promoted config for the detected bank sequentially until one successfully parses transactions.
- **FR-013**: System MUST cache a successfully used AI-generated configuration locally in the preferences store (keyed by bankId) so that re-importing from the same bank does not require another AI call. Remote Config-delivered configs remain in their existing in-memory cache; the local cache is only for interim AI-generated configs not yet promoted. Once a config is promoted and delivered via Remote Config, the local cache entry becomes redundant.
- **FR-014**: System MUST gracefully fall back to existing full-AI parsing when config generation fails (network error, invalid response, ReDoS-vulnerable regex, zero parsed transactions).

### Key Entities

- **ParserCandidate**: A crowd-validated parser configuration awaiting promotion. Contains bankId, parserConfig (full schema), anonymizedSample (sanitized table rows), userId_hash, successCount, createdAt, and status (candidate/promoted/rejected).
- **AnonymizedSample**: A sanitized representation of statement table rows with personal data replaced by placeholders. Preserves structural patterns (column layout, date/amount formats) needed to validate regex correctness.
- **ReDoS Validation Result**: The outcome of regex safety analysis — pass (safe to use) or fail (contains vulnerable patterns, with reason).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can import statements from previously unsupported banks in under 30 seconds (compared to full-AI parsing which processes each transaction individually).
- **SC-002**: At least 80% of AI-generated parser configurations successfully parse all transactions in a statement on first attempt (no fallback to full-AI needed).
- **SC-003**: No personal financial data (merchant names, account numbers, card numbers) is present in any candidate record stored in the shared pool — 100% anonymization rate.
- **SC-004**: Zero ReDoS-vulnerable regex patterns are stored in the shared candidate pool or delivered to users via promoted configurations.
- **SC-005**: After a configuration is promoted, all users receive the new bank support within 1 hour without an app update.
- **SC-006**: The number of supported banks grows organically as users import statements from new banks, with at least 2 new banks added via auto-promotion within the first 3 months of deployment.
- **SC-007**: Repeat imports from the same unsupported bank (by the same user) use the locally cached config without additional AI calls — zero redundant AI requests.

## Assumptions

- Gemini 2.5 Flash is capable of generating valid regex patterns and ParserConfig-compatible JSON from a small sample of PDF table rows with sufficient accuracy (>80%).
- The existing ParserConfig schema is expressive enough to represent parser configurations for the majority of Kazakh bank statement formats.
- Users importing from the same bank will encounter structurally similar PDF formats (same table layout, column order), making a single regex configuration viable across users.
- The configuration refresh interval (1 hour) is acceptable latency for promoted config delivery.
- The server-side function for auto-promotion has appropriate permissions to update the official configuration list programmatically.
- The anonymization approach (replacing merchant names, removing account numbers) is sufficient to protect user privacy for this iteration.
- The promotion threshold of 3 independent users provides adequate confidence that a configuration is correct, balancing speed of adoption against false positive risk.

## Out of Scope

- Server-side AI review agent for candidate validation before promotion.
- Human-in-the-loop approval workflow for promoted configurations.
- User feedback mechanism ("this parsing is incorrect") for post-import corrections.
- Rollback mechanism for promoted configurations that turn out to be faulty.
- Support for non-PDF statement formats (CSV, OFX, etc.) via this auto-update pipeline.
- Multi-language prompt support for Gemini (currently Russian-focused for Kazakh banks).
