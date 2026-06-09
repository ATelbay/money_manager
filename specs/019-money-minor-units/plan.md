# Implementation Plan: Money as Integer Minor Units (Double → Long)

**Spec ID**: 019-money-minor-units · **Branch**: `019-money-minor-units` · **Date**: 2026-06-08
**Inputs**: [spec.md](./spec.md) · [research.md](./research.md) · [data-model.md](./data-model.md)

---

## Summary

Convert all monetary amounts from `Double` to `Long` minor units at a **fixed scale of 2**
across `core:model`, `core:database` (Room 8→9), `data:*`, `domain:statistics`/`domain:import`,
`domain:exchangerate`, `presentation:*`, and the Firestore sync layer — with backward/forward
compatible cloud sync and a lossless local migration. Exchange-rate coefficients stay `Double`.

The drift bug is fixed by exact integer arithmetic on a still-materialized, still-incremental
account balance (D3). Cloud compatibility is achieved by dual-writing a new encrypted `*Minor`
DTO field alongside the legacy major field and reading `*Minor`-first (D4). The `uniqueHash`
cross-version dedup is preserved by reconstructing the legacy amount string inside the hash
(R1).

---

## Technical Context

- **Language/Build**: Kotlin, Gradle (Version Catalogs + Convention Plugins), 43 modules.
- **Storage**: Room 2.8.x (schema export on, `MigrationTestHelper`), Preferences DataStore.
- **Sync**: Firestore via encrypted `String` DTO fields (`FieldCipher` AES-GCM; `encryptLong`/
  `decryptLong` already present).
- **Currency math**: `BigDecimal` (`ConvertAmountUseCase`); rates `Double`.
- **UI**: Compose + Material 3; `core:ui` formatters hardcode 2 fraction digits today.
- **Tests**: JUnit + MockK + Turbine (JVM); Roborazzi screenshot tests; instrumented
  `MigrationTest` + Compose UI tests.
- **Current schema version**: 8 (latest export `8.json`). Target: 9.

No NEEDS CLARIFICATION remain (all resolved in research D1–D4, R1–R7).

---

## Architecture / Constitution Check

No `.specify/memory/constitution.md` exists; the governing contract is `AGENTS.md`. Gate review:

- ✅ **Layer boundaries**: change is type-only and stays within existing dependency edges
  (`presentation → domain → core:model`; `data → domain + core:database`). No new cross-layer
  deps. `domain:statistics`/`domain:import` keep using DAOs directly (intentional).
- ✅ **`core:model` is the single home** for the money type and conversion helpers; all layers
  depend down onto it.
- ✅ **No new dependencies** — uses existing `BigDecimal`, Room, cipher.
- ✅ **No hardcoded strings/colors/dp** introduced; display strings unchanged.
- ✅ **testTags** untouched; UI-automation testability preserved.
- ✅ **Sync isolation invariants** (owner-guard, anon-skip, push-before-persist, delete
  idempotency, `uniqueHash` dedup) preserved — verified per-site in research.
- ⚠️ **Documented exceptions**: `ExchangeRate.quotes` stays `Double` (coefficients);
  `AmountParser.parseAmount` stays `Double` (transient string extraction, converted at the
  `ParsedTransaction` boundary). Both recorded in research R3/R4 and acceptable under SC-002
  (which targets stored money *fields*).

**Result**: PASS. No unjustified violations.

---

## Implementation Phases

Ordered bottom-up so each phase compiles on top of the previous. Each phase ends green before
the next. (Granular task list is produced by `/speckit.tasks`.)

### Phase 1 — Foundation: `core:model` helpers + model field types
- Add `MoneyConversions.kt` (helpers, `MINOR_UNIT_SCALE`, overflow guard) + unit tests
  (round-trip HALF_UP, overflow, `parseToMinorUnitsOrNull`).
- Flip money fields to `Long` on `Transaction`, `Account`, `Budget`, `Debt`, `DebtPayment`,
  `RecurringTransaction`, `ParsedTransaction`, `TransactionOverride`.
- **Gate**: `core:model` compiles; helper tests green.

### Phase 2 — `core:database`: entities, DAOs, migration, schema, MigrationTest
- Entities REAL→INTEGER (`Long`); `updateBalance(delta: Long)`; SUM queries → `Long`/`Long?`.
- `Migration_8_9` (6 tables, rename/copy/cast pattern, preserve indexes/FKs/uniqueHash); bump
  version 8→9; build to emit `9.json`; **commit `9.json`**; register migration in `DatabaseModule`.
- Extend `MigrationTest`: 8→9 with fractional `Double` seed values asserting exact `Long`;
  add full-chain validation; penny-drift / FR-013 balance==SUM assertion fixture.
- **Gate**: `:core:database:assembleDebug` + `:core:database:connectedAndroidTest` green.

### Phase 3 — `core:common` hashing + `core:crypto` confirm
- Retype `generateTransactionHash(amountMinor: Long, …)` reconstructing legacy string (R1);
  hash-stability unit test. Confirm `encryptLong`/`decryptLong` available (no change expected).
- **Gate**: hash test green; cross-version digest equality asserted.

### Phase 4 — `data:*` mappers + repositories + balance arithmetic
- Retype all mappers (transactions/accounts/budgets/debts/recurring) to pass `Long`.
- Balance apply/revert on `Long` (`TransactionRepositoryImpl.kt:91,113,116,136`).
- Mapper round-trip tests (major→minor→major) green.
- **Gate**: all `data:*` modules compile + unit tests green.

### Phase 5 — `domain:*`: statistics, transactions repo, budgets, import, exchangerate
- `StatisticsModels` + `GetPeriodSummaryUseCase` on `Long` (incl. internal day/month maps).
- `TransactionRepository.observeExpenseSumByCategory(): Flow<Long>` **interface + impl** in
  lock-step with the now-`Flow<Long>` DAO (cross-layer pair).
- `domain:budgets`: `SaveBudgetUseCase(monthlyLimit: Long)`; `GetBudgetsWithSpendingUseCase`
  spend/remaining on `Long` and `percentage` via `toMajorDouble()` ratio (avoid integer division).
- Import: `AmountParser` and the AI-response DTO stay `Double`; convert to `Long` minor once,
  used for both the `uniqueHash` call and `ParsedTransaction` construction;
  `ImportTransactionsUseCase` balance delta on `Long`.
- `ConvertAmountUseCase(amountMinor: Long): Long` (BigDecimal algorithm R3); rewrite its 9 tests.
- **Gate**: domain modules (`statistics`, `transactions`, `budgets`, `import`, `exchangerate`)
  + tests green; import balance test green; budget percentage test green.

### Phase 6 — Sync: `core:firestore` DTOs + `data:sync`
- Add `*Minor` encrypted fields to 6 DTOs; push dual-writes; pull reads `*Minor`-first with
  legacy fallback (D4).
- `PullSyncUseCase` balance delta on `Long`; preserve delete idempotency / dedup.
- DTO mapper tests: dual-write present, read preference, legacy `Double` doc back-compat,
  plaintext v0 fallback.
- **Gate**: `core:firestore` + `data:sync` compile + tests green; forward/back-compat tests pass.

### Phase 7 — `presentation:*` + `core:ui`
- `core:ui` `formatAmount`/cards/items/`SummaryStatCard` accept `Long` minor; local screen
  formatters retyped (R6).
- Input parse sites → `parseToMinorUnitsOrNull`; pre-fill → `toMajorPlainString` (R5).
- Presentation State fields → `Long`; VMs (`TransactionListViewModel`, statistics resolver,
  debts) compute on `Long`, feed charts via `toMajorDouble()`.
- Update `TransactionListViewModelTest`, `StatisticsCurrencyDisplayResolverTest`.
- Re-record Roborazzi screenshots if pixels shift (expected identical).
- **Gate**: all `presentation:*` compile + unit/UI tests green.

### Phase 8 — App build, docs, full verification
- `./gradlew assembleDebug test` (+ `:core:database:connectedAndroidTest`) green.
- Grep gate (SC-002): no `Double` money field in `domain/`, `data/`, `core:model` (except
  `ExchangeRate.quotes`).
- Document the ×100 scale in `AGENTS.md` and project memory.
- **Manual**: real Firestore test account — pull legacy docs, push dual-write, read back, verify
  amounts and balance integrity before merge.
- **Gate**: all SC-001…SC-008 satisfied.

---

## Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| `uniqueHash` mismatch → duplicate rows on sync | Reconstruct legacy `amount/100.0` string in hash (R1); hash-stability test; stored hashes not recomputed in migration |
| Old app version on 2nd device breaks on new docs | Dual-write legacy field (D4); old client ignores unknown `*Minor` field |
| SQLite `ROUND` precision on artifact-laden Doubles | `CAST(ROUND(col*100.0) AS INTEGER)` ≡ HALF_UP; positives only for amounts; FR-013 surfaces drift; documented limitation |
| Pre-existing materialized-balance drift carried forward | FR-013 invariant check `balance == SUM(txns)` flags affected accounts at migration boundary |
| Migration is irreversible + touches sync | Feature branch only; full MigrationTest + drift + sync compat tests + manual Firestore check before merge |
| Vico chart needs `Number`/`Double` | Convert `Long → toMajorDouble()` only at the chart-point build site, never stored |

---

## Out of Scope (restated)

Per-currency exponent storage; derived `SUM` balance; >2-decimal currencies; changing
`ExchangeRate.quotes` type; unifying duplicate screen formatters onto `MoneyDisplayFormatter`;
dropping legacy Firestore fields (future cleanup release); any new user-facing feature.

---

## Artifacts

- `spec.md` — requirements, decisions, success criteria.
- `research.md` — D1–D4 confirmed + R1–R7 findings/decisions.
- `data-model.md` — exact field/column/DTO/state changes with file:line.
- `plan.md` — this file.
- **Next**: `/speckit.tasks` → `tasks.md` (dependency-ordered, per-phase task breakdown).
