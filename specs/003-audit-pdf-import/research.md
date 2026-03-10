# Research: PDF Import Audit & Bereke Fix

**Feature**: 003-audit-pdf-import | **Date**: 2026-03-10

---

## Decision: Bereke Bug Root Cause Hypotheses

**Decision**: Full diagnosis requires running PdfBox against `bereke_statement.pdf` and capturing raw text output. The unit tests use hardcoded strings that do NOT go through PdfBox, so the actual text output may differ.

**Three ranked hypotheses** (to confirm in Phase 0 diagnostic test):

### H1 (Most Likely): `joinContinuationLines` misidentifies continuation lines
The `joinContinuationLines` heuristic uses `^\s*\d{2}\.\d{2}\.\d{2,4}` as the "new record" signal. Any line NOT starting with a date is appended to the previous line. Real PdfBox output from a 7-column table may produce:
- Page numbers between transactions (e.g., "2" or "Page 2 of 5") that don't match the date pattern → get appended to the previous transaction
- Repeated column headers at page breaks that partially match or don't match `skipPatterns`
- Lines starting with a date-like number from another column (unlikely but possible)

### H2 (Likely): Real PDF has amount format variation
The `comma_dot` parser does `amountStr.replace(",", "").toDouble()`. For "10,000.00" → "10000.00" → 10000.0. This is correct. But if the real PDF outputs amounts as "10 000.00" (space separator) or "10,000" (no decimal), the regex `[\d,]+\.\d{2}` won't match.

### H3 (Possible): Missing `skipPatterns` for real PDF content
The current Bereke `skipPatterns`: "Transaction date", "Card account statement", "For the period", "Available as of", "Debit total", "Credit total". If the real PDF has additional lines (page headers, account info lines, totals with different text) not covered by these patterns, those lines contaminate `joinContinuationLines`.

### H4 (Less Likely): PdfBox column ordering
With `sortByPosition=true`, PdfBox sorts text by Y then X position. For a 7-column table, each row's text should appear on the same line. However, if the PDF uses overlapping bounding boxes or non-standard column layout, text might appear out of expected order. This would produce a line where date and operation are separated by unexpected tokens.

---

## Decision: Integration Test Architecture

**Decision**: JVM tests in `core/parser/src/test/` using PdfBox `PDDocument` + `PDFTextStripper` directly, bypassing `PdfTextExtractor`'s Android context dependency.

**Rationale**:
- `PDFBoxResourceLoader.init(context)` loads CMap font resources from Android assets — needed only for non-Latin character sets (CJK, Arabic). Standard Kazakh bank statements use Latin-1 or UTF-8 with standard fonts and work without initialization.
- Tests run in `./gradlew :core:parser:test` — no device/emulator required, fast CI feedback.
- Test PDFs placed in `core/parser/src/test/resources/` — accessible via classloader.

**Alternatives considered**:
- Instrumented tests (`androidTest`): Rejected — requires device, slower, adds CI complexity.
- Mock `PdfTextExtractor`: Rejected — defeats the purpose; we need the real extraction layer.
- Abstract `PdfTextExtractor` to accept bytes as input: Rejected — over-engineering; direct PdfBox call in test utility is simpler.

**Implementation**: Create `PdfTestHelper.kt` in test sources:
```kotlin
object PdfTestHelper {
    fun extractText(resourceName: String): String {
        val bytes = PdfTestHelper::class.java.classLoader!!
            .getResourceAsStream(resourceName)!!.readBytes()
        val doc = PDDocument.load(bytes)
        return PDFTextStripper().apply { sortByPosition = true }.getText(doc)
            .also { doc.close() }
    }
}
```

---

## Decision: Parser Config Fix Delivery

**Decision**: Fix both `default_parser_config.json` (local fallback) AND Firebase Remote Config (`parser_configs` key).

**Rationale**: Local fallback covers offline scenarios and new installs before Remote Config fetches. Remote Config covers all existing installs without requiring an app update.

**Deployment sequence**: Fix local JSON first (shipped with app update), then update Remote Config immediately after release to cover existing users.

---

## Decision: Share Intent — No Code Changes Needed

**Decision**: Existing `PendingNavigationManager` + `MoneyManagerApp` code correctly handles all three app states.

**Evidence**:
- Cold start: `MainActivity.onCreate()` extracts PDF URI and enqueues `NavigationAction.OpenImport` before `setContent`. NavHost settles → action fires.
- Warm: `MainActivity.onNewIntent()` re-enqueues the action. Same flow.
- Onboarding: `LaunchedEffect(backStackEntry?.id, pendingAction)` has `if (!onboardingCompleted) return@LaunchedEffect` — action is held until onboarding flag becomes `true` and NavHost settles.

**Action**: Manual verification only (P3-1 checklist). No code changes.

---

## Key Code Findings

### `RegexStatementParser.kt`
- `parse()` processes lines sequentially; MULTILINE regex option applies `^`/`$` per-line.
- `joinContinuationLines()` date pattern: `^\s*\d{2}\.\d{2}\.\d{2,4}` — joins anything not starting with a date.
- `parseAmount("comma_dot")`: removes ALL commas, converts to Double. "10,000.00" → 10000.0 ✓
- `matchToTransaction()`: for `useNamedGroups=true` (Bereke, Eurasian), uses `match.groups["name"]?.value`.

### Bereke Regex Analysis
Pattern: `^(?<date>\d{2}\.\d{2}\.\d{4})\s+(?<operation>Operation|Payment for goods and(?:\s+services)?|Card replenishment through(?:\s+Bereke Bank|\s+payment terminal)?|Transfer from a card(?:\s+through Bereke Bank)?)\s+(?<details>.+?)\s+(?<sign>[-]?)(?<amount>[\d,]+\.\d{2})(?:\s.*)?$`

Known behavior (unit tests confirm):
- `(?<details>.+?)` is non-greedy — captures minimum text before first sign+amount match.
- `(?:\s.*)?$` absorbs trailing content (currency code, second amount, card number, continuation words).
- The pattern handles both "Payment for goods and" (split across lines) and "Payment for goods and services" (full name on one line).

**Potential regex weakness**: If `(?<details>.+?)` finds an earlier amount-like substring before the real amount (e.g., a phone number like "700-123-45"), it could mis-assign `details` and `amount`. The "10-4" in "ROYAL PETROL AZS 10-4" doesn't trigger this because "-4" doesn't match `\.\d{2}`. To be verified against real output.

### PdfTextExtractor
- Single singleton; `PDFBoxResourceLoader` init happens once via `by lazy`.
- On exception: returns empty string and logs with Timber — this is why Gemini fallback triggers silently!
- If the extraction throws (e.g., due to Android context not being initialized in JVM context), the parser silently gets empty string → zero transactions → Gemini fallback.

**Important**: In production (Android), `PDFBoxResourceLoader.init(context)` IS called. If it fails to init and causes extraction to return empty, that's also a potential root cause. Must verify.

### ParseStatementUseCase AI Fallback Trigger
Fallback fires when `RegexParseResult` is null (from `StatementParser.tryParsePdf()`) or when zero transactions are returned from regex parsing. This is a silent promotion to AI — no log distinguishes "bank not detected" from "regex parsed 0 transactions" from "PDF extraction failed".

**Recommendation**: Add diagnostic logging to distinguish the three failure modes. This helps confirm the bug and future debugging.
