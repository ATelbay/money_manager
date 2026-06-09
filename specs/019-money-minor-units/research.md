# Research: Money as Integer Minor Units (Double → Long)

**Spec**: [spec.md](./spec.md) · **Branch**: `019-money-minor-units` · **Date**: 2026-06-08

This document resolves the open technical questions surfaced during codebase exploration. Each
section: **Decision → Rationale → Alternatives considered**. The four spec-level decisions
(D1–D4) are confirmed here against real code; R1–R6 are new findings the original brief did not
anticipate.

---

## D1 — Fixed scale ×100 for all currencies (confirmed)

**Decision**: Store every money value as `Long` hundredths (scale 2), currency-independent.

**Rationale (validated against code)**: The migration is a pure per-row arithmetic transform
with no currency JOIN. Crucially, `core:ui` does **not** currently have per-currency fraction
logic — `defaultMoneyNumberFormat()` (`MoneyDisplayFormatter.kt:90-94`) hardcodes
`min/maxFractionDigits = 2` for *all* currencies, and the inline formatters in `BalanceCard.kt`,
`AccountCard.kt`, and the local `DecimalFormat("#,##0.##")` helpers all assume 2 decimals.
Therefore displaying `minor / 100.0` with 2 fraction digits is **byte-identical** to today's
output. The brief's premise that "MoneyDisplayFormatter already knows decimal places" is
slightly off — it doesn't — but the conclusion still holds: fixed ×100 is the lowest-risk
choice and display does not regress.

**Alternatives**: per-currency exponent (rejected — needs currency context per row at migration
and per-row scale at every arithmetic site, for zero real benefit given all supported
currencies are 2-dp and display is uniformly 2-dp today).

---

## D2 — `Long`, scale 2, overflow guarded (confirmed)

**Decision**: `Long` everywhere; major→minor helper guards overflow; document the range.

**Range**: `Long.MAX = 9_223_372_036_854_775_807` minor = `±9.22e16` major units. A defensive
check in `majorToMinor` throws `ArithmeticException` if `abs(major) > 9.2e16` rather than
silently overflowing. No `BigInteger`.

---

## D3 — Materialized balance on `Long` (confirmed)

**Decision**: Keep `accounts.balance` materialized + incremental (`AccountDao.updateBalance`,
`AccountDao.kt:34`), but `delta` and `balance` become `Long`. Integer add/subtract is exact, so
the drift disappears.

**Validated**: Balance arithmetic lives in exactly four families of call sites —
`TransactionRepositoryImpl.kt:91,113,116,136` (insert/update-revert/update-apply/delete),
`ImportTransactionsUseCase.kt:115`, and `PullSyncUseCase.kt:252`. All use the pattern
`if (type == "income") amount else -amount`. `transactions.amount` is stored as a **positive
magnitude**; the sign is applied at balance time via the `type` discriminator. This is important
(see R4: migration rounding is always on positive amounts).

**Integrity net (FR-013)**: A test asserts, per account, `balance == SUM(signed amounts)` after
migration. Pre-existing drift in the stored balance is surfaced, not silently carried.

**Alternative**: derived `SUM(transactions)` — rejected; larger change, alters balance sync.

---

## D4 — Sync wire format: dual-write at the DTO-mapper level (refined)

**Finding that changes the implementation**: Firestore money is NOT a raw `Double` field. Each
DTO carries money as an **encrypted `String`** (`TransactionDto.amount: String`, etc.). The push
mappers call `cipher.encryptDouble(value)` (or `value.toString()` plaintext fallback); pull
mappers call `cipher.decryptDouble(...)` (or `.toDouble()`). The cipher **already exposes
`encryptLong`/`decryptLong`** (`AesGcmFieldCipher.kt:35-36` per FieldCipher), so no crypto work
is needed.

**Decision**: Add a **new optional encrypted field per money-bearing DTO**, e.g.
`amountMinor: String? = null` (and `balanceMinor`, `monthlyLimitMinor`, `totalAmountMinor`).
- **Push (dual-write)**: write BOTH the legacy `amount` (`encryptDouble(minor/100.0)`) AND the
  new `amountMinor` (`encryptLong(minor)`).
- **Pull (read)**: if `amountMinor` is present and non-blank → `decryptLong` → use directly;
  else fall back to legacy `amount` → `decryptDouble` → `majorToMinor(...)`.

**Rationale**: An old app version on a second device only knows the legacy field and keeps
working (Firestore `toObject` ignores unknown fields, so the extra `amountMinor` is harmless to
it). The new app prefers the exact `Long` field. This fully satisfies Scenarios 4 and 5 without
a heuristic. (A content heuristic — "string contains `.` ⇒ legacy major, else minor" — also
works because `Double.toString()` always emits a `.` and `Long.toString()` never does, but an
explicit field is clearer and is the chosen approach.)

**Sync invariants preserved**: owner-guard (`SyncManager.kt:71+`), anon-skip
(`LoginSyncOrchestrator.kt:50`), push-before-persist (`SyncManager.kt:83`), delete idempotency
(`PullSyncUseCase.kt:251`), `uniqueHash` dedup (`PullSyncUseCase.kt:277`) are all untouched by
the field-type change — except `uniqueHash` itself, see R1.

**Future cleanup (separate release)**: once old clients are retired, drop the legacy fields and
make `*Minor` the sole money field.

---

## R1 — `uniqueHash` includes `amount` (CRITICAL, new finding)

**Finding**: `TransactionHashGenerator.generateTransactionHash(date, amount: Double, type,
details)` builds `"$date|$amount|$type|${details.take(30)}"` then SHA-256
(`TransactionHashGenerator.kt:6-14`). `amount` is interpolated as a `Double`, so its string form
(`"100.0"`, `"12345.67"`) is part of the hash. The hash is:
- stored encrypted on `TransactionEntity.uniqueHash` (unique index, `TransactionEntity.kt:28`),
- used for cross-device dedup on pull (`PullSyncUseCase.kt:277`),
- computed identically by **old app versions** still in the field.

If the new app feeds `Long` minor (`10000`) into the hash, it produces a different digest than
the old app's `Double` (`100.0`) for the same transaction → **duplicate rows on sync**.

**Decision**: Keep the hash input **representation-stable**. New signature
`generateTransactionHash(date, amountMinor: Long, type, details)` reconstructs the legacy
canonical string internally: `val amount = amountMinor / 100.0` then the **same**
`"$date|$amount|$type|..."` interpolation. For all clean ≤2-dp amounts (the universe of real
input) `Double.toString(amountMinor / 100.0)` reproduces the original `Double.toString(amount)`,
so digests match across app versions and across the migration. Existing stored `uniqueHash`
values are therefore **not** recomputed by `Migration_8_9` (they remain valid).

**Residual risk (documented)**: a stored legacy amount whose `Double.toString` differs from
`(round(amount*100)/100.0).toString()` (pathological precision artifacts) would hash
differently. These are vanishingly rare for user-entered 2-dp money; the worst case is a single
duplicate row, not data loss. A unit test asserts hash stability for a representative set
(integers, one-dp, two-dp, `0.1+0.2`-style values).

**Alternative rejected**: recompute/migrate stored hashes — impossible cleanly in SQL (hashes
are encrypted) and would still mismatch old-app hashes, breaking cross-version dedup.

---

## R2 — SQLite `ROUND` vs `RoundingMode.HALF_UP`

**Finding/Decision**: `Migration_8_9` converts each column with
`CAST(ROUND(<col> * 100.0) AS INTEGER)`. SQLite's `ROUND()` rounds halves **away from zero**,
which is exactly `RoundingMode.HALF_UP`. Since `transactions.amount` (and all the other money
columns except `balance`) are stored as **positive magnitudes**, rounding is on positives where
HALF_UP and away-from-zero trivially agree. `balance` may be negative, but away-from-zero ==
HALF_UP there too, so the migration and the Kotlin `majorToMinor` helper are consistent.

**Caveat**: `ROUND(double * 100.0)` itself uses IEEE-754 math, so a stored value already
carrying a representation artifact (e.g. `12.005` held as `12.00499…`) may round to `1200`
rather than `1201`. This reflects the *actual* stored value; the migration captures best-effort
truth and FR-013's invariant check surfaces any account-level inconsistency. Documented as an
accepted limitation.

---

## R3 — Currency conversion on minor units (`ConvertAmountUseCase`)

**Current** (`ConvertAmountUseCase.kt:30-47`): `invoke(amount: Double, source, target,
quotes: Map<String,Double>): Double`. Converts via `BigDecimal` (`.toString()` to avoid IEEE
artifacts), `amount × sourceRate ÷ targetRate` with `SCALE=2, HALF_UP`, returns `Double`. Quotes
are KZT-per-unit rates.

**Decision**: New signature `invoke(amountMinor: Long, source, target, quotes): Long`. Algorithm:
1. Same-currency short-circuit returns `amountMinor` unchanged.
2. `val major = BigDecimal(amountMinor).movePointLeft(2)` (exact, no FP).
3. `result = major × sourceRate ÷ targetRate` in `BigDecimal` (rates via `BigDecimal(rate.toString())`),
   divide with generous intermediate scale (e.g. 10) `HALF_UP`.
4. `return result.movePointRight(2).setScale(0, HALF_UP).toLong()` → minor units.

Rates (`ExchangeRate.quotes: Map<String, Double>`) stay `Double` — they are coefficients.
The existing 9 unit tests are rewritten to assert `Long` minor (e.g. `105.26` → `10526`).

---

## R4 — `AmountParser` (core:parser) stays `Double`; convert at the import boundary

**Finding**: `AmountParser.parseAmount(amountStr, format): Double` (`core:parser`) extracts a
number from bank-statement text; bank parser tests assert `Double` with `0.01` delta.

**Decision**: Leave `AmountParser` returning `Double` (it is a string-extraction utility, not a
money store or accumulator — analogous to the transient `BigDecimal` used inside conversion).
Convert `Double → Long` minor **once**, where `ParsedTransaction(amount = …)` is constructed in
the import pipeline (`ParseStatementUseCase` / `ImportTransactionsUseCase`), via `majorToMinor`.
`ParsedTransaction.amount` (the stored model field) becomes `Long`. This keeps the bank parser
test suite intact and confines the type change to the model boundary.

**Grep-clean note (SC-002)**: SC-002 targets money **fields** in `domain`/`data`/`core:model`.
A transient `Double` return inside `core:parser` extraction is documented as permitted (like
exchange-rate coefficients and intermediate `BigDecimal`s).

---

## R5 — Presentation input parsing: single choke point per form

**Finding**: Every form already sanitizes input to digits + a single `.` and parses with
`toDoubleOrNull()`:
`TransactionEditViewModel.kt:127-141/189`, onboarding `CreateAccountViewModel.kt:56/71`,
`BudgetEditViewModel.kt:87/108`, `DebtEditBottomSheet.kt:203`, `PaymentBottomSheet.kt:142`,
`RecurringEditViewModel.kt:101/177`, import `ParsedTransactionItem.kt`.

**Decision**: Replace each `String.toDoubleOrNull()` parse with a shared
`String.parseToMinorUnitsOrNull(): Long?` helper (in `core:model`, backed by `BigDecimal` +
`majorToMinor`) at the existing parse site. Pre-fill (edit) sites that currently do
`value.toBigDecimal().stripTrailingZeros().toPlainString()` switch to a
`minorToMajorPlainString(Long)` helper so the text field shows the same string. No new choke
points are introduced; the existing ones change type.

---

## R6 — Duplicate local formatters in presentation

**Finding**: Several screens format money with their own helpers instead of
`MoneyDisplayFormatter`: `AccountListScreen.kt:300` (`DecimalFormat("#,##0.##")`),
`BudgetListScreen.kt:276`, `DebtListScreen.kt:475`, `DebtDetailScreen.kt:525` (K/M abbreviation).
`BalanceCard.kt` and `AccountCard.kt` also have inline `NumberFormat` blocks.

**Decision**: These helpers change their parameter from `Double` to `Long` minor and divide by
`100.0` before formatting (preserving their existing abbreviation/precision behavior). Scope does
**not** require unifying them onto `MoneyDisplayFormatter` (out of scope — no behavior change);
they are only retyped. The core `formatAmount(amount: Double, …)` in `MoneyDisplayFormatter.kt`
gains a `Long`-minor overload (or its callers divide), keeping the rendered string identical.

---

## R7 — Screenshot & unit tests to update

- **ConvertAmountUseCaseTest** (9 tests) → assert `Long` minor.
- **DTO mapper tests** (`TransactionDtoMapperTest`, `AccountDtoMapperTest`, …) → assert dual
  field write + read preference + legacy fallback.
- **ImportTransactionsUseCaseTest** (`updateBalance(-100.0)` → `-10000L`).
- **TransactionListViewModelTest**, **StatisticsCurrencyDisplayResolverTest** → `Long` assertions.
- **Bank parser tests** → unchanged (parser stays `Double`).
- **Screenshot tests** (`CategoryBottomSheetScreenshotTest`, `TransactionEditScreenScreenshotTest`):
  state holds `amount = "15000"` as a text string — unaffected by the type change; re-record only
  if rendered pixels shift (they should not, given identical display). Note: these `.png`s are
  already modified in the working tree from prior work.
- **New tests**: `MigrationTest` 8→9; mapper round-trip (major→minor→major) HALF_UP; penny-drift
  regression (balance over a series of `0.1/0.2`-style txns); hash-stability (R1); sync
  forward/back-compat (R2/D4).

---

## Summary of decisions

| ID | Decision |
|----|----------|
| D1 | Fixed scale ×100, currency-independent storage |
| D2 | `Long` + overflow guard, range documented |
| D3 | Materialized balance, incremental, on `Long`; FR-013 invariant check |
| D4 | Dual-write new `*Minor` encrypted DTO field + legacy field; read `*Minor`-first |
| R1 | `uniqueHash` reconstructs legacy `amount/100.0` string for cross-version stability |
| R2 | `CAST(ROUND(col*100.0) AS INTEGER)` ≡ HALF_UP; positives ⇒ no tie ambiguity |
| R3 | `ConvertAmountUseCase(amountMinor: Long): Long` via `BigDecimal` |
| R4 | `AmountParser` stays `Double`; convert at `ParsedTransaction` construction |
| R5 | Shared `String.parseToMinorUnitsOrNull()` at existing parse sites |
| R6 | Local screen formatters retyped to `Long`, divide by 100; no unification |
| R7 | Test update matrix + new migration/drift/hash/sync tests |
