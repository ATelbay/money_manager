# Code Review Task List

Tasks created from branch review of `004-ai-regex-autoupdate`.

## Execution Order

1. `CR-001` Fix the broken `ParseStatementUseCase` test setup so the target test task compiles again.
2. `CR-002` Align Firestore rules with the implemented Android submission flow and the existing spec contract.
3. `CR-003` Pass header context into AI parser-config generation so bank identification is stable.
4. `CR-005` Make short but usable statement samples eligible for AI config generation.
5. `CR-004` Preserve multiple cached configs for the same bank and add coverage for same-bank variants.

## Parallelization Notes

- `CR-001` should go first because it blocks targeted verification for the import domain tests.
- `CR-002` can be implemented independently from the parser/AI work.
- `CR-003`, `CR-004`, and `CR-005` touch the same parser/import pipeline and should be done together in one change set.
- Verification should re-run targeted tests for `domain:import` and `core:parser` after all parser/import changes land.

## CR-001 Fix broken ParseStatementUseCase unit test

- Priority: critical
- Files:
  - `domain/import/src/test/java/com/atelbay/money_manager/domain/importstatement/usecase/ParseStatementUseCaseTest.kt`
- Problem:
  - `ParseStatementUseCase` now requires `parserConfigProvider`, but the test still uses the old constructor signature and does not compile.
- Suggested fix:
  - Add a `ParserConfigProvider` mock to the test setup.
  - Stub `isAiFullParseEnabled()` and any other required methods explicitly.
  - Re-run `:domain:import:testDebugUnitTest`.
- Acceptance criteria:
  - `ParseStatementUseCaseTest` compiles.
  - The targeted Gradle test task passes.

## CR-002 Align Firestore rules with Android candidate submission flow

- Priority: critical
- Files:
  - `firestore.rules`
  - `core/firestore/src/main/java/com/atelbay/money_manager/core/firestore/dto/ParserCandidateDto.kt`
  - `core/firestore/src/main/java/com/atelbay/money_manager/core/firestore/datasource/FirestoreDataSourceImpl.kt`
  - `domain/import/src/main/java/com/atelbay/money_manager/domain/importstatement/usecase/SubmitParserCandidateUseCase.kt`
- Problem:
  - Firestore rules require `contributors` on create and update, but the Android client never sends or updates that field.
  - As written, candidate creation and success-count increment will both be rejected by security rules.
- Suggested fix:
  - Either remove the `contributors` requirement from rules and keep the simpler contract from the spec, or implement `contributors` end-to-end in DTO, datasource, use case, and increment flow.
  - Ensure the chosen contract is consistent across spec, rules, Android client, and Cloud Function.
- Acceptance criteria:
  - A signed-in client can create a new parser candidate successfully.
  - A signed-in client can increment `successCount` for an existing candidate successfully.
  - Emulator tests or manual verification prove the rules allow the intended flow and reject invalid updates.

## CR-003 Pass bank-identifying context into AI parser config generation

- Priority: critical
- Files:
  - `core/ai/src/main/java/com/atelbay/money_manager/core/ai/GeminiService.kt`
  - `core/ai/src/main/java/com/atelbay/money_manager/core/ai/GeminiServiceImpl.kt`
  - `domain/import/src/main/java/com/atelbay/money_manager/domain/importstatement/usecase/ParseStatementUseCase.kt`
  - `core/parser/src/main/java/com/atelbay/money_manager/core/parser/StatementParser.kt`
- Problem:
  - The AI prompt only receives post-header sample rows, so Gemini cannot reliably infer bank identity or produce stable `bank_id` and `bank_markers`.
  - This breaks FR-006 and risks unstable cache keys, duplicate candidates, and bad promotion grouping.
- Suggested fix:
  - Extend the API to pass both a header snippet and transaction sample rows to Gemini.
  - Update the prompt to include bank-identifying header/content plus the transaction sample.
  - Add tests that verify known-bank normalization still works when only the header contains the bank name.
- Acceptance criteria:
  - AI config generation has access to bank-identifying text.
  - `bank_id` normalization and `bank_markers` generation follow FR-006.
  - Tests cover the case where the bank name is present only in the header.

## CR-004 Preserve multiple cached AI configs for the same bank

- Priority: high
- Files:
  - `domain/import/src/main/java/com/atelbay/money_manager/domain/importstatement/usecase/ParseStatementUseCase.kt`
- Problem:
  - The local cache currently removes all configs with the same `bankId` before storing a new one.
  - That conflicts with the multi-variant design where one bank can have several valid parser configs tried sequentially.
- Suggested fix:
  - Cache multiple configs per bank, keyed at least by `bankId + transactionPattern`.
  - Keep dedup logic narrow enough to replace only exact duplicates.
  - Add test coverage for two different configs under the same bank.
- Acceptance criteria:
  - Two different parser variants for the same bank can coexist in local cache.
  - Repeat imports can use either cached variant without forcing a new AI generation call.

## CR-005 Do not skip AI config generation for short but usable samples

- Priority: high
- Files:
  - `core/parser/src/main/java/com/atelbay/money_manager/core/parser/StatementParser.kt`
  - `core/parser/src/test/java/com/atelbay/money_manager/core/parser/StatementParserTest.kt`
- Problem:
  - `extractSampleRows()` returns an empty string when fewer than 5 post-header lines exist.
  - The contract says the system should use all available lines after the header in that case.
- Suggested fix:
  - Return all available non-empty lines after the header, up to the sample cap, even when fewer than 5 lines exist.
  - Keep the empty result only for truly empty or header-only documents.
  - Update tests to match the contract.
- Acceptance criteria:
  - Short statements still produce a non-empty AI sample when usable lines exist.
  - Header-only or blank PDFs still return an empty sample.
  - Tests reflect the intended contract.
