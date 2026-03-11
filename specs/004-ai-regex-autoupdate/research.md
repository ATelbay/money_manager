# Research: AI-Driven Parser Auto-Update

**Date**: 2026-03-11 | **Branch**: `004-ai-regex-autoupdate`

## R1: Gemini Structured Output for ParserConfig Generation

**Decision**: Use Firebase AI SDK's `responseSchema` with `Schema.obj()` builder API.

**Rationale**: The existing `GeminiServiceImpl` already sets `responseMimeType = "application/json"` but has no `responseSchema`. Adding a schema guarantees the response conforms to the ParserConfig field structure at the API level, eliminating JSON parse errors and reducing fallback rate.

**Key Details**:
- Schema builder: `com.google.firebase.ai.type.Schema` — uses `Schema.obj()`, `Schema.string()`, `Schema.array()`, `Schema.enumeration()`, etc.
- All fields in `Schema.obj()` are **required** by default (use `optionalProperties` for optional ones).
- No regex pattern constraints at schema level — format expectations (dateFormat patterns, transactionPattern syntax) must be described in the prompt text.
- Schema size counts toward input token limits — keep it minimal.
- Supported subset of OpenAPI 3.0: `enum`, `items`, `maxItems`, `nullable`, `properties`, `required`. No `oneOf`, `allOf`, `pattern`, etc.

**Alternatives Considered**:
- Free-form JSON with post-validation: simpler but higher failure rate (~20-30% malformed responses).
- Custom response parser with retry: adds latency, complexity.

## R2: ReDoS Detection Strategy

**Decision**: Two-layer approach — lightweight heuristic check at runtime + `withTimeout` guard.

**Rationale**: Full NFA-analysis libraries (recheck, SafeRegex) require Scala runtime (~6 MB) — unacceptable for Android APK. A heuristic checker catches the most dangerous patterns (nested quantifiers, overlapping alternations), and a coroutine timeout prevents any regex from blocking the app regardless.

**Key Details**:
- **Heuristic checks** (static string analysis of regex pattern):
  1. Nested quantifiers: `(X+)+`, `(X*)*`, `(X+)*` — scan for quantifiers inside quantified groups
  2. Overlapping alternations under quantifier: `(A|B)+` where A and B overlap
  3. Adjacent overlapping quantified groups: `\d+\d+`
- **Runtime guard**: `withTimeout(5_000)` on regex parsing operations
- **Build-time (CI)**: Add recheck as `testImplementation` to run comprehensive NFA analysis in unit tests

**Alternatives Considered**:
- recheck as runtime dependency: too heavy for Android (Scala stdlib ~6 MB)
- SonarQube S5852: CI-only, doesn't help with dynamically generated regex
- No detection (trust Gemini): unacceptable security risk

## R3: BankId Normalization in AI Prompt

**Decision**: Include normalization rules directly in the Gemini prompt with known bank name mappings and a slug generation rule.

**Rationale**: Relying on Gemini to be consistent across users is unreliable. A deterministic normalization instruction in the prompt (lowercase, latin transliteration, underscores, e.g., "halyk_bank") ensures consistent grouping in the candidate pool.

**Key Details**:
- Prompt includes mapping of 5 existing banks: kaspi, freedom, forte, bereke, eurasian
- For unknown banks: instruction to generate slug as `lowercase_latin_transliteration_with_underscores`
- Examples in prompt: "Народный банк" → "narodniy_bank", "Jusan Bank" → "jusan_bank"

**Alternatives Considered**:
- Client-side header hashing: loses human-readable bankId
- Two-phase (AI extract + deterministic normalizer): adds complexity, harder to extend

## R4: Config Matching for Candidate Deduplication

**Decision**: Match candidates by `bankId + transactionPattern` (exact string equality). Auxiliary fields ignored.

**Rationale**: The transactionPattern (regex) is the core discriminator — if two configs have the same regex, they parse the same statement format. Auxiliary fields like operationTypeMap or skipPatterns may differ between AI generations but don't change parsing correctness.

**Alternatives Considered**:
- Full ParserConfig equality: too strict, would fragment candidates due to minor AI variations
- bankId-only matching: too loose, would merge genuinely different statement formats
- Fuzzy similarity: complex to implement, hard to reason about

## R5: Local Cache Storage

**Decision**: Store AI-generated ParserConfigs in Preferences DataStore as serialized JSON string keyed by `ai_parser_configs`.

**Rationale**: DataStore is already used in the project (core:datastore), lightweight, survives app restarts, no schema migration needed. ParserConfig is already `@Serializable`, so encoding/decoding is trivial.

**Key Details**:
- Single DataStore key `ai_parser_configs` containing a JSON array of ParserConfigs
- Keyed internally by `bankId` — when looking up, deserialize and filter
- On promotion (config appears in Remote Config), the local entry becomes redundant
- Constitution check: DataStore is acceptable for small key-value data; the serialized configs list is small (few KB max)

**Alternatives Considered**:
- Room table: overkill for a handful of configs, requires migration
- File storage: harder to manage lifecycle
- SharedPreferences: deprecated in favor of DataStore

## R6: Firestore Collection Design for Candidates

**Decision**: Top-level `parser_candidates` collection (not under `users/{userId}`).

**Rationale**: Candidates are shared across all users — they need a global collection for cross-user aggregation (successCount incrementing). The existing `users/{userId}/...` pattern is for per-user data. Candidate records contain only hashed userId, not sensitive user data.

**Key Details**:
- Collection: `parser_candidates`
- Document ID: auto-generated
- Security rules: any authenticated user can create; updates restricted to `successCount` increment only; reads allowed for matching bankId
- The Cloud Function reads this collection to check thresholds

**Alternatives Considered**:
- `users/{userId}/parser_candidates`: requires cross-user queries (collection group queries), more complex security rules
- Realtime Database: not already in the project, no benefit

## R7: Cloud Function for Auto-Promotion

**Decision**: Firebase Cloud Function (Node.js/TypeScript) triggered by Firestore `onWrite` on `parser_candidates` collection.

**Rationale**: Cloud Functions integrate natively with Firestore triggers. When `successCount` is updated, the function checks if it meets the threshold, and if so, reads the current Remote Config value, appends the new ParserConfig, and publishes.

**Key Details**:
- Trigger: `onDocumentUpdated("parser_candidates/{candidateId}")`
- Reads promotion threshold from Remote Config (or hardcoded default: 3)
- Uses Firebase Admin SDK to update Remote Config `parser_configs` key
- Skips promotion if bankId + transactionPattern already exists in the official list
- Logs promotion events for monitoring

**Alternatives Considered**:
- Scheduled function (check all candidates periodically): adds latency, more complex
- Client-side promotion: security risk, requires Admin SDK access
