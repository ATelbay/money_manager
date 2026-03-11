# Quickstart: AI-Driven Parser Auto-Update

**Branch**: `004-ai-regex-autoupdate`

## Prerequisites

- Android Studio with Kotlin 2.3.0
- Firebase project configured (AI Logic, Firestore, Remote Config)
- Node.js 18+ (for Cloud Function)
- Access to Firebase console for Remote Config and Firestore rules

## Dev Setup

1. **Checkout branch**:
   ```bash
   git checkout 004-ai-regex-autoupdate
   ```

2. **Verify build**:
   ```bash
   ./gradlew assembleDebug
   ```

3. **Cloud Function setup** (new):
   ```bash
   cd functions/
   npm install
   npm run build
   firebase deploy --only functions
   ```

4. **Firestore rules** (update):
   ```bash
   firebase deploy --only firestore:rules
   ```

## Key Files to Modify

| Module | File | Change |
|--------|------|--------|
| core:ai | `GeminiService.kt` | Add `generateParserConfig()` method |
| core:ai | `GeminiServiceImpl.kt` | Implement with structured output schema |
| core:parser | `StatementParser.kt` | Add `extractSampleRows()` method |
| core:parser | `RegexValidator.kt` | **New file**: ReDoS heuristic checker |
| core:parser | `SampleAnonymizer.kt` | **New file**: anonymization logic |
| core:firestore | `FirestoreDataSource.kt` | Add `parser_candidates` methods |
| core:firestore | `FirestoreDataSourceImpl.kt` | Implement candidate CRUD |
| core:firestore | `ParserCandidateDto.kt` | **New file**: Firestore DTO |
| core:datastore | `UserPreferences.kt` | Add `ai_parser_configs` key |
| core:remoteconfig | `FirebaseParserConfigProvider.kt` | Merge promoted configs |
| domain:import | `ParseStatementUseCase.kt` | Insert AI config generation step |
| domain:import | `SubmitParserCandidateUseCase.kt` | **New file**: candidate submission |
| presentation:import | `ImportViewModel.kt` | Handle AI generation state + candidate submission |
| functions/ | `src/promote-candidate.ts` | **New directory**: Cloud Function |

## Testing Strategy

### Unit Tests
- `RegexValidatorTest.kt` — ReDoS detection with known-vulnerable patterns
- `SampleAnonymizerTest.kt` — anonymization rules (merchants, accounts, dates)
- `ParseStatementUseCaseTest.kt` — AI config generation fallback flow
- `SubmitParserCandidateUseCaseTest.kt` — candidate submission + dedup logic

### Integration Tests
- Import a PDF from a "new" bank → verify AI config is generated and used
- Verify candidate appears in Firestore emulator
- Verify Cloud Function promotes at threshold

## Architecture Notes

- **No new Gradle modules** — extend existing core:parser, core:ai, core:firestore, core:datastore, domain:import, presentation:import
- **ParserConfig model unchanged** — AI generates the same schema
- **Firestore collection is global** — `parser_candidates` (not per-user)
- **Cloud Function is the only new project** — `functions/` at repo root
