# Feature Specification: Money as Integer Minor Units (Double → Long)

**Spec ID**: 019-money-minor-units
**Status**: Draft
**Created**: 2026-06-08
**Branch**: `019-money-minor-units`

---

## Overview

Migrate every monetary amount in MoneyManager from binary floating-point (`Double`) to
exact integer **minor units** (`Long`) across the whole stack: domain models, Room storage,
mappers/repositories, statistics, import, currency conversion, presentation input/formatting,
and Firestore cloud sync.

Money is **stored and computed** as whole minor units (Long) and **formatted to major units
only at the UI edge**. Currency *conversion* keeps using `BigDecimal` (as today in
`ConvertAmountUseCase`). Exchange *rates* (`ExchangeRate.quotes`) stay `Double` — they are
coefficients, not money.

This is a correctness/data-integrity migration with no new end-user feature surface. The user
visible outcome is that balances and totals stop drifting and stay penny-accurate forever.

---

## Problem Statement

All money is currently stored as `Double`: `Transaction.amount`, `Account.balance`, plus
`Budget`, `Debt`, `DebtPayment`, `RecurringTransaction`, and `ParsedTransaction`. Account
balance is **materialized** in the `accounts` table and updated **incrementally**
(`AccountDao.updateBalance: balance = balance + :delta`).

Binary `Double` cannot represent decimal fractions exactly (`0.1 + 0.2 != 0.3`). Across many
CRUD operations the rounding error accumulates and is **persisted irreversibly** into the
materialized balance and into individual amounts. Over time a user's displayed balance no
longer equals the exact sum of their transactions, and there is no way to recover the true
value. The goal is to make all money arithmetic exact.

---

## Decisions (resolved in this spec)

These were flagged as open in the brief. They are resolved here with rationale so planning can
proceed; they can be revisited in `/speckit.clarify`.

### D1 — Fixed scale ×100 for all currencies (chosen)

Store every amount as **hundredths** (minor units at a fixed exponent of 2), regardless of the
account currency.

- **Rationale**: The Room/Firestore migration becomes a pure per-row arithmetic transform
  (`round(value × 100)`) with **no need to know each row's currency** — no JOIN from
  transaction → account → currency at migration time. Per-currency display precision is already
  handled downstream by `MoneyDisplayFormatter`, which knows each currency's real fraction
  digits, so a fixed storage scale does not degrade display.
- **Rejected alternative**: per-currency exponent (KZT/USD = 2, JPY = 0, BHD = 3…). More
  semantically precise but the transaction migration would require resolving each row's
  currency (JOIN), Firestore back-fill would need currency context per document, and every
  arithmetic site would need the per-row scale. Higher migration risk for negligible gain given
  the app's supported currencies are all 2-digit in practice.
- **Consequence**: For a zero-decimal currency (e.g. JPY) the two trailing zeros are always
  `00`; `MoneyDisplayFormatter` renders 0 fraction digits. For a 3-decimal currency the third
  digit is not representable — out of scope (no such currency is supported today; documented as
  a known limitation in **Assumptions**).

### D2 — Type `Long`, fixed scale 2

`Long` holds ±9.22e18 minor units = ±9.22e16 major units (≈ 92 quadrillion). This is far beyond
any realistic personal balance. The overflow ceiling is **documented** in `core:model` and a
defensive guard is added in the major→minor helper (see FR-002). No `BigInteger`.

### D3 — Balance stays materialized + incremental, but on exact `Long` (chosen)

Keep `accounts.balance` materialized and updated incrementally (`balance = balance + :delta`),
but with `delta` and `balance` as `Long`.

- **Rationale**: On `Long`, integer addition/subtraction is **exact** — the accumulation drift
  that motivated this work disappears entirely. A derived balance (`SUM(transactions)`) would
  also remove drift but is a larger change: it alters how balances sync, removes a column other
  code/sync reads, and changes query cost. The smaller safe change that fully fixes the bug is
  preferred.
- **Rejected alternative**: derived `SUM(transactions)`. Revisit later if desired; not needed
  to fix the correctness bug.
- **Safety net**: A migration-time and test-time invariant check verifies, for each account,
  `materialized balance == SUM(non-deleted transaction amounts)` after conversion (see FR-013),
  catching any pre-existing drift at the migration boundary rather than carrying it forward
  silently.

### D4 — Sync wire format: dual-write Long + keep legacy Double; read with Long-first fallback

On **push**, write the new `Long` minor-unit field **and** keep writing the legacy `Double`
major field (dual-write). On **pull**, read the `Long` field if present; otherwise fall back to
converting the legacy `Double` field. Optionally stamp a document `amountFormatVersion`.

- **Rationale**: Other devices may still run the **old app version** that only reads the legacy
  `Double` field; dual-write keeps those clients working (forward compatibility) without
  corrupting their data. Reading Long-first preserves exactness when both fields exist. This
  honors the brief's "do not damage already-logged-in users' data."
- **Consequence**: Legacy fields can be dropped in a future release once old clients are gone
  (documented as future cleanup). Sync isolation invariants (owner-guard, anon-skip,
  push-before-persist, `uniqueHash` dedup, delete idempotency) are unchanged.

---

## Scope

### In scope

1. **core:model** — `Double → Long` (minor units) on `Transaction.amount`, `Account.balance`,
   `Budget` (limit/spent), `Debt` (total/paid/remaining), `DebtPayment.amount`,
   `RecurringTransaction.amount`, `ParsedTransaction.amount`. New conversion helpers
   `majorToMinor(BigDecimal/Double/String)` and `minorToMajor` with HALF_UP rounding, plus the
   fixed `MINOR_UNIT_SCALE = 100` constant and overflow guard.
2. **core:database** — money columns `REAL → INTEGER`; new `Migration_8_9` (current schema
   version **8**) converting existing `amount`/`balance` via `ROUND(value × 100)`; exported
   schema `9.json`; `MigrationTest` extended to cover 8→9. `AccountDao.updateBalance(delta: Long)`;
   SUM/aggregate queries (`observeExpenseSumByCategory`, statistics sums) return `Long`.
3. **data:*** — mappers do lossless round-trips; sign (income +, expense −) and revert/apply
   balance logic operate on `Long`.
4. **domain:statistics** and **domain:import** (use DAOs directly) — recompute on `Long`.
5. **domain:exchangerate** — `ConvertAmountUseCase` takes/returns minor units, converting via
   `BigDecimal` accounting for source and target scale.
6. **presentation:*** — parse user-entered amount strings → minor units at a single choke point
   per form (`TransactionEditViewModel.setAmount/save`, plus Account/Budget/Debt forms); format
   minor → major in `core:ui` (`MoneyDisplayFormatter`, `formatAmount`, `BalanceCard`,
   `IncomeExpenseCard`, conversion in `TransactionListViewModel`, statistics, debts).
7. **Sync (core:firestore + data:sync)** — dual-write/read-fallback per D4; preserve all sync
   isolation invariants.
8. **Docs** — record the chosen scale in `AGENTS.md` and project memory.

### Out of scope

- Per-currency exponent storage (D1 rejects it).
- Derived `SUM(transactions)` balance (D3 rejects it).
- Supporting currencies with >2 decimal places.
- Changing exchange-rate storage type (`quotes` stay `Double`).
- Any new user-facing feature, screen, or visual redesign beyond identical-or-better numeric
  display.
- Dropping the legacy Double Firestore fields (future cleanup release).

---

## User Scenarios & Testing

### Scenario 1: Existing local data migrates without loss
**Actor**: An existing user upgrading the app
1. User has transactions and account balances stored as `Double` on schema v8.
2. User installs the new version; Room runs `Migration_8_9`.
3. Every amount/balance is converted to minor units via `ROUND(value × 100)`.
- **Acceptance**: After migration, every displayed amount equals the pre-migration displayed
  amount (to the currency's precision); no transaction or balance is lost or shifted.

### Scenario 2: Penny arithmetic no longer drifts
**Actor**: A user doing many small transactions
1. User creates a long series of transactions with fractional amounts (e.g. repeated 0.1 / 0.2
   style values) and edits/deletes some.
2. The displayed account balance equals the exact sum of the account's transactions at all times.
- **Acceptance**: A regression test reproducing the original `Double` drift scenario shows zero
  drift on `Long`.

### Scenario 3: Currency conversion stays correct
**Actor**: A user viewing totals converted to a display currency
1. Amounts in minor units are converted between currencies via `ConvertAmountUseCase`.
- **Acceptance**: Converted results match the pre-migration values within rounding tolerance;
  conversion uses `BigDecimal` with correct source/target scale and HALF_UP.

### Scenario 4: Cloud sync reads old documents (back-compat)
**Actor**: A logged-in user with pre-existing Firestore documents (amounts as `Double` major)
1. The new app pulls documents that have no `Long` minor field.
2. The app reads the legacy `Double` field and converts it to minor units.
- **Acceptance**: Pulled amounts are correct; no document is corrupted; sync isolation
  invariants hold.

### Scenario 5: Cloud sync stays readable by old app version (forward-compat)
**Actor**: A user running the old app on a second device
1. The new app pushes a document with both the new `Long` field and the legacy `Double` field.
2. The old app reads the legacy `Double` field as before.
- **Acceptance**: The old client displays correct amounts; the new client prefers the `Long`
  field on read-back.

### Scenario 6: New input is parsed once into minor units
**Actor**: A user entering an amount in any money form
1. User types "1234.56" into the transaction/account/budget/debt amount field.
2. The string is parsed to `123456` minor units at a single choke point.
- **Acceptance**: Round-trip (typed string → minor → displayed) shows the same value; locale
  decimal separators are handled; invalid input is rejected as today.

---

## Functional Requirements

### Core model & helpers
- **FR-001**: All monetary fields in `core:model` (`Transaction.amount`, `Account.balance`,
  `Budget`, `Debt`, `DebtPayment`, `RecurringTransaction`, `ParsedTransaction`) MUST be `Long`
  representing minor units at fixed scale 2. No money field in `domain`, `data`, or `core:model`
  MAY remain `Double` (exchange-rate `quotes` excepted).
- **FR-002**: `core:model` MUST expose conversion helpers: major→minor (from `BigDecimal`,
  `String`, and `Double`) using `RoundingMode.HALF_UP`, and minor→major (to `BigDecimal`). The
  major→minor helper MUST guard against `Long` overflow and document the supported range.
- **FR-003**: Sign semantics MUST be preserved on `Long`: income amounts positive, expense
  amounts negative; revert/apply balance deltas on create/update/delete MUST be exact integer
  arithmetic.

### Database & migration
- **FR-004**: All money columns MUST change `REAL → INTEGER`; the Room schema version MUST
  increment 8 → 9 with an exported `9.json` schema.
- **FR-005**: `Migration_8_9` MUST convert each existing money value to minor units via
  `ROUND(value × 100)` (banker-safe rounding consistent with HALF_UP), for every affected
  table/column.
- **FR-006**: `AccountDao.updateBalance` MUST accept a `Long` delta; aggregate queries
  (`observeExpenseSumByCategory` and all statistics SUM queries) MUST return `Long`.
- **FR-007**: `MigrationTest` MUST cover 8→9 with realistic values including fractional `Double`
  inputs, asserting exact converted results.

### Statistics & import
- **FR-008**: `domain:statistics` and `domain:import` MUST perform all sums/aggregations on
  `Long` minor units.

### Currency conversion
- **FR-009**: `ConvertAmountUseCase` MUST accept and return minor units, converting through
  `BigDecimal` using the source and target scale and HALF_UP, with `quotes` remaining `Double`.

### Presentation
- **FR-010**: Each money form MUST parse the user's amount string into minor units at a single
  choke point (`TransactionEditViewModel.setAmount/save`, Account/Budget/Debt forms).
- **FR-011**: All amount rendering MUST format minor → major at the UI edge via
  `MoneyDisplayFormatter`/`formatAmount` and the shared components (`BalanceCard`,
  `IncomeExpenseCard`, list/statistics/debts), with per-currency fraction digits unchanged or
  improved.

### Sync
- **FR-012**: Firestore push MUST dual-write the `Long` minor field and the legacy `Double`
  major field; pull MUST read the `Long` field if present and otherwise convert the legacy
  `Double` field. All sync isolation invariants (owner-guard, anon-skip, push-before-persist,
  `uniqueHash` dedup, delete idempotency) MUST be preserved.

### Integrity
- **FR-013**: After migration, for each account the materialized balance SHOULD equal the SUM
  of its non-deleted transactions' amounts; a test MUST assert this invariant on representative
  data.

### Invariants that MUST NOT regress
- **FR-014**: `Account.createdAt` default `0`, real timestamp set in `AccountRepositoryImpl`.
- **FR-015**: Categories obtained via `GetCategoriesUseCase`, not `TransactionRepository`.
- **FR-016**: Soft-delete + sync-of-deletes behavior unchanged.

---

## Success Criteria

- **SC-001**: `./gradlew assembleDebug test` is green; `MigrationTest` covers 8→9.
- **SC-002**: A grep over `domain/`, `data/`, and `core:model` finds **no** `Double`-typed money
  field (exchange-rate `quotes` is the only permitted `Double`).
- **SC-003**: Existing local data migrates with zero amount loss or distortion (verified by
  migration test on fractional inputs).
- **SC-004**: A penny-series regression test that drifts under the old `Double` implementation
  shows **exactly zero** drift under `Long`.
- **SC-005**: Mapper round-trip (major→minor→major) is lossless under HALF_UP for representative
  values.
- **SC-006**: Sync forward/back-compat tests pass: reading a legacy `Double` document yields the
  correct minor-unit value; a pushed document contains both fields.
- **SC-007**: UI shows amounts identically to (or more precisely than) before; screenshot tests
  are updated and pass.
- **SC-008**: Existing cloud data for a real logged-in test account migrates correctly (manual
  verification against a real Firestore account before merge).

---

## Key Entities

- **Transaction** — `amount: Long` (minor units; sign = income/expense direction).
- **Account** — `balance: Long` (minor units, materialized + incremental).
- **Budget** — limit/spent in minor units (`Long`).
- **Debt** — total/paid/remaining in minor units (`Long`).
- **DebtPayment** — `amount: Long`.
- **RecurringTransaction** — `amount: Long`.
- **ParsedTransaction** — `amount: Long` (import boundary).
- **ExchangeRate** — `quotes` remain `Double` (coefficients, not money).

---

## Assumptions

- All currencies the app stores are effectively 2-decimal; a fixed scale of 2 is sufficient
  (D1). Currencies with >2 decimals are not supported and are out of scope.
- HALF_UP is the canonical rounding mode everywhere money crosses a precision boundary
  (parse, migrate, convert), matching existing `ConvertAmountUseCase` intent.
- `Long` range is more than sufficient; overflow is guarded and documented rather than handled
  with `BigInteger`.
- Some Firestore documents written by old clients lack a minor-unit field; D4's read-fallback
  covers them.
- The pre-existing materialized balance may already contain accumulated drift; the migration
  captures the converted historical value as-is, and FR-013's invariant check surfaces (rather
  than silently hides) any account where balance ≠ SUM(transactions).

---

## Rollout / Risk

- The migration is **irreversible** and touches **sync**: develop on this branch only.
- Run `MigrationTest` (8→9) plus the penny-drift and round-trip tests before merge.
- Manually verify against a **real Firestore test account** (pull legacy docs, push dual-write,
  read back) before merge.
- Document the chosen scale (×100) in `AGENTS.md` and project memory.
- Future cleanup (separate release): drop legacy `Double` Firestore fields once old clients are
  retired.

---

## Dependencies

- Existing `ConvertAmountUseCase` (`BigDecimal`-based) in `domain:exchangerate`.
- `MoneyDisplayFormatter` in `core:ui` (already per-currency precision aware).
- Room schema export + `MigrationTest` infrastructure in `core:database`.
- Firestore sync modules (`core:firestore`, `data:sync`) and the sync isolation invariants
  recorded in project memory (`project_sync_isolation_invariants`).
