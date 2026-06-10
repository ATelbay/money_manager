# Tasks: 019-money-minor-units

**Feature**: Money as Integer Minor Units (Double → Long)
**Branch**: `019-money-minor-units`
**Created**: 2026-06-08
**Inputs**: [spec.md](./spec.md) · [plan.md](./plan.md) · [research.md](./research.md) · [data-model.md](./data-model.md)

---

## Workstreams

> NOTE: Unlike a multi-feature spec, this is a single irreversible migration. The workstreams
> below are **bottom-up and sequential** — each one must compile + pass tests before the next
> (a half-migrated app does not compile). They map 1:1 to plan.md phases. `[P]` marks tasks
> within the *same* workstream that touch different files and can run in parallel.

| ID | Workstream | plan phase | Depends on |
|----|------------|-----------|------------|
| US1 | core:model helpers + model field types | Phase 1 | — |
| US2 | core:database entities, DAOs, Migration_8_9, MigrationTest | Phase 2 | US1 |
| US3 | core:common hashing + core:crypto confirm | Phase 3 | US1 |
| US4 | data:* mappers + repositories + balance arithmetic | Phase 4 | US2, US3 |
| US5 | domain: statistics, transactions repo, budgets, import, exchangerate | Phase 5 | US4 |
| US6 | sync: core:firestore DTOs + data:sync | Phase 6 | US4, US3 |
| US7 | presentation:* + core:ui | Phase 7 | US5 |
| US8 | app build, docs, full verification | Phase 8 | US1–US7 |

---

## Phase 1: Setup

- [X] T001 Confirm work is on branch `019-money-minor-units` and the working tree builds (`./gradlew :core:model:assembleDebug`) before starting. No new modules or `gradle/libs.versions.toml` entries are needed for this migration.

---

## Phase 2: Foundational — `core:model` helpers + model field types (US1)

### Goal: Establish the `Long` minor-unit type and conversion helpers so every other layer can depend on them.

### Independent test criteria:
- `:core:model` compiles standalone.
- Conversion helper unit tests green (round-trip HALF_UP, overflow guard, string parse).

### Helpers
- [X] T002 [US1] Create `core/model/src/main/java/com/atelbay/money_manager/core/model/money/MoneyConversions.kt` — `const val MINOR_UNIT_SCALE = 2`, `MAX_MAJOR_UNITS`, and helpers `BigDecimal.toMinorUnits(): Long` (`setScale(2, HALF_UP)`, overflow-guarded via `longValueExact`), `majorToMinor(Double): Long`, `majorToMinor(BigDecimal): Long`, `String.parseToMinorUnitsOrNull(): Long?` (sanitize digits + single `.`/`,` → BigDecimal → toMinorUnits), `Long.toMajor(): BigDecimal`, `Long.toMajorDouble(): Double`, `Long.toMajorPlainString(): String`. See data-model.md "Conversion helpers".
- [X] T003 [US1] Create `core/model/src/test/java/com/atelbay/money_manager/core/model/money/MoneyConversionsTest.kt` — round-trip major→minor→major HALF_UP (incl. `0.1`, `0.2`, `12345.67`, integers), HALF_UP tie cases, `parseToMinorUnitsOrNull` valid/invalid/locale-comma, overflow throws `ArithmeticException` above `MAX_MAJOR_UNITS`.

### Model field types (all in `core/model/src/main/java/com/atelbay/money_manager/core/model/`)
- [X] T004 [P] [US1] `Transaction.kt:5` — `amount: Double` → `Long` (positive magnitude).
- [X] T005 [P] [US1] `Account.kt:7` — `balance: Double` → `Long` (leave `createdAt: Long = 0`).
- [X] T006 [P] [US1] `Budget.kt:9-11` — `monthlyLimit`, `spent`, `remaining` Double → `Long` (keep `percentage: Float`).
- [X] T007 [P] [US1] `Debt.kt:7-9` — `totalAmount`, `paidAmount` (default `0L`), `remainingAmount` Double → `Long`.
- [X] T008 [P] [US1] `DebtPayment.kt:6` — `amount: Double` → `Long`.
- [X] T009 [P] [US1] `RecurringTransaction.kt:5` — `amount: Double` → `Long`.
- [X] T010 [P] [US1] `ParsedTransaction.kt:7` — `amount: Double` → `Long`.
- [X] T011 [P] [US1] `TransactionOverride.kt:6` — `amount: Double? = null` → `Long? = null`.
- [X] T012 [US1] Build gate: `./gradlew :core:model:test` green.

---

## Phase 3: `core:database` — entities, DAOs, Migration_8_9, MigrationTest (US2)

### Goal: Convert money columns REAL→INTEGER and ship a lossless 8→9 migration with tests.

### Independent test criteria:
- `:core:database:assembleDebug` compiles; `9.json` exported and committed.
- `MigrationTest` 8→9 green (fractional Double seeds → exact Long).
- Penny-drift / balance==SUM invariant test green.

### Entities (`core/database/src/main/java/com/atelbay/money_manager/core/database/entity/`)
- [X] T013 [P] [US2] `TransactionEntity.kt:35` — `amount: Double` → `Long`.
- [X] T014 [P] [US2] `AccountEntity.kt:18` — `balance: Double` → `Long`.
- [X] T015 [P] [US2] `BudgetEntity.kt:26` — `monthlyLimit: Double` → `Long`.
- [X] T016 [P] [US2] `DebtEntity.kt:27` — `totalAmount: Double` → `Long`.
- [X] T017 [P] [US2] `DebtPaymentEntity.kt:33` — `amount: Double` → `Long`.
- [X] T018 [P] [US2] `RecurringTransactionEntity.kt:33` — `amount: Double` → `Long`.

### DAO signatures (`core/database/src/main/java/com/atelbay/money_manager/core/database/dao/`)
- [X] T019 [US2] `AccountDao.kt:34-35` — `updateBalance(accountId, delta: Long, updatedAt)`.
- [X] T020 [US2] `TransactionDao.kt:129-130` — `observeExpenseSumByCategory(...): Flow<Long>`, `COALESCE(SUM(amount), 0)`.
- [X] T021 [US2] `DebtPaymentDao.kt:26-27` — `sumAmountByDebtId(...): Flow<Long?>`.

### Migration + registration + schema
- [X] T022 [US2] Bump `MoneyManagerDatabase.kt:33` version `8` → `9` (keep `exportSchema = true`).
- [X] T023 [US2] Create `core/database/src/main/java/com/atelbay/money_manager/core/database/migration/Migration_8_9.kt` — for each of the 6 tables (`transactions`, `accounts`, `budgets`, `debts`, `debt_payments`, `recurring_transactions`) do rename→create-new(INTEGER)→`INSERT … SELECT CAST(ROUND(<moneyCol> * 100.0) AS INTEGER)` for the money column and verbatim copy for all other columns→drop-old→rename; **recreate every index/FK** (notably `transactions.uniqueHash` unique index and account/debt FKs). Copy `uniqueHash` as-is (research R1). Follow `Migration_7_8.kt` structure.
- [X] T024 [US2] Register migration in `core/database/src/main/java/com/atelbay/money_manager/core/database/di/DatabaseModule.kt:68` — add `MIGRATION_8_9` to `.addMigrations(...)` and import it.
- [X] T025 [US2] Run `./gradlew :core:database:assembleDebug` to generate `core/database/schemas/com.atelbay.money_manager.core.database.MoneyManagerDatabase/9.json`; verify all 6 money columns show affinity `INTEGER`; **commit `9.json`**.

### Migration tests (`core/database/src/androidTest/java/com/atelbay/money_manager/core/database/migration/`)
- [X] T026 [US2] Add `migrate8To9_convertsMoneyToMinorUnits` in `MigrationTest.kt` — seed v8 rows with fractional REAL values (e.g. `amount = 1999.99`, `balance = 0.1 + 0.2`-style, `12345.67`) across all 6 tables; run `runMigrationsAndValidate(TEST_DB, 9, true, MIGRATION_8_9)`; assert each stored value equals `round(value*100)` as `Long` (e.g. `199999`). Follow `migrate7To8_*` pattern.
- [X] T027 [US2] Extend the full-chain test (`migrateAll3To8` → `migrateAll3To9`) to validate through v9.
- [X] T028 [US2] Add `migrate8To9_preservesUniqueHash` — assert `transactions.uniqueHash` values are unchanged across the migration.
- [X] T029 [US2] Build gate: `./gradlew :core:database:connectedAndroidTest` green.

---

## Phase 4: `core:common` hashing + `core:crypto` confirm (US3)

### Goal: Keep uniqueHash digest-stable across app versions (research R1); confirm Long cipher.

### Independent test criteria:
- Hash-stability test green: `generateTransactionHash` from `Long` minor reproduces the legacy `Double`-based digest.

- [X] T030 [US3] `core/common/src/main/java/com/atelbay/money_manager/core/common/TransactionHashGenerator.kt:6-14` — change signature to `generateTransactionHash(date, amountMinor: Long, type, details)`; internally `val amount = amountMinor / 100.0`; keep the exact `"$date|$amount|$type|${details.take(30)}"` interpolation + SHA-256.
- [X] T031 [US3] Update all call sites of `generateTransactionHash` to pass `Long` minor (search repo; expected in transactions repo/import/parse paths — note `ParseStatementUseCase.kt:733` currently passes a `Double`, see T049).
- [X] T032 [US3] Add `core/common/src/test/java/com/atelbay/money_manager/core/common/TransactionHashGeneratorTest.kt` — assert the digest for `amountMinor` (e.g. `10000`, `1234567`, `10`, `30`) equals the digest computed from the legacy `Double` (`100.0`, `12345.67`, `0.1`, `0.3`) for the same date/type/details.
- [X] T033 [US3] Confirm `core/crypto/.../FieldCipher.kt` exposes `encryptLong`/`decryptLong` and `AesGcmFieldCipher.kt` implements them; no change expected (note in PR if absent).

---

## Phase 5: `data:*` mappers + repositories + balance arithmetic (US4)

### Goal: Round-trip money losslessly entity↔domain; balance apply/revert exact on Long.

### Independent test criteria:
- All `data:*` modules compile; mapper round-trip tests green.

### Mappers
- [X] T034 [P] [US4] `data/transactions/.../mapper/TransactionMapper.kt:11,25` — pass-through stays valid (Long↔Long); confirm types compile.
- [X] T035 [P] [US4] `data/accounts/.../mapper/AccountMapper.kt:10,18` — `balance` Long pass-through.
- [X] T036 [P] [US4] `data/budgets/.../mapper/BudgetMapper.kt:13-15,21` — `monthlyLimit` Long; `spent = 0L`; `remaining = monthlyLimit`.
- [X] T037 [P] [US4] `data/debts/.../mapper/DebtMapper.kt:9,12-14,30` — `remaining = (totalAmount - paidAmount)`; `remainingAmount = remaining.coerceAtLeast(0L)`; all Long.
- [X] T038 [P] [US4] `data/debts/.../mapper/DebtPaymentMapper.kt:9,18` — `amount` Long.
- [X] T039 [P] [US4] `data/recurring/.../mapper/RecurringTransactionMapper.kt:19,46` — `amount` Long.

### Balance arithmetic
- [X] T040 [US4] `data/transactions/.../repository/TransactionRepositoryImpl.kt:91,113,116,136` — delta `if (type == "income") amount else -amount` (and reverts) on `Long`; calls `updateBalance(_, delta: Long, _)`.

### Tests
- [X] T041 [P] [US4] Add mapper round-trip unit tests (`data/transactions`, `data/accounts`, `data/debts`) — major→minor→major lossless under HALF_UP; follow `/unit-testing` skill.
- [X] T042 [US4] Build gate: `./gradlew :data:transactions:test :data:accounts:test :data:budgets:test :data:debts:test :data:recurring:test` green.

---

## Phase 6: `domain:` statistics, transactions repo, budgets, import, exchangerate (US5)

### Goal: All domain sums/conversions on Long; the transaction-sum repository contract returns Long; budget spending recomputed on Long; import converts at the ParsedTransaction boundary.

### Independent test criteria:
- Domain modules + tests green; `observeExpenseSumByCategory` returns `Flow<Long>` through the repository; budget percentage computes correctly; `ConvertAmountUseCase` returns Long; import balance delta is Long.

### Statistics
- [X] T043 [P] [US5] `domain/statistics/.../model/StatisticsModels.kt:8,14,20,36,37` — `CategorySummary.totalAmount`, `DailyTotal.amount`, `MonthlyTotal.amount`, `PeriodSummary.totalExpenses`, `totalIncome` → `Long`.
- [X] T044 [US5] `domain/statistics/.../usecase/GetPeriodSummaryUseCase.kt:90,117,160,182` — retype the internal day/month aggregation maps (`Map<Long, Double>` / `Map<Pair<Int,Int>, Double>`), the `fillDays`/`fillMonths` helpers, and the `total: Double` / `items: List<Pair<Long, Double>>` params to `Long`; `.sumOf { it.amount }` returns `Long`.

### Transactions repository contract (G1 — was DAO-only before)
- [X] T045 [US5] `domain/transactions/.../repository/TransactionRepository.kt:33` — change `observeExpenseSumByCategory(...): Flow<Double>` → `Flow<Long>`; update impl `data/transactions/.../repository/TransactionRepositoryImpl.kt:147-152` to match the now-`Flow<Long>` DAO (T020). (Cross-layer: interface in domain, impl in data — keep them in lock-step.)

### Budgets (G2/G3 — domain:budgets was missing entirely)
- [X] T046 [US5] `domain/budgets/.../usecase/SaveBudgetUseCase.kt:12,29` — `monthlyLimit: Double` → `Long`; `remaining = monthlyLimit` stays Long.
- [X] T047 [US5] `domain/budgets/.../usecase/GetBudgetsWithSpendingUseCase.kt:25-45` — `spent` (from `observeExpenseSumByCategory`, now `Long`) and `remaining = (monthlyLimit - spent).coerceAtLeast(0L)` on `Long`; compute `percentage` as a ratio via majors: `(spent.toMajorDouble() / monthlyLimit.toMajorDouble()).toFloat().coerceAtLeast(0f)` (NOT integer division), guarded by `monthlyLimit > 0L`.
- [X] T048 [P] [US5] Update any `domain:budgets` unit tests (e.g. `GetBudgetsWithSpendingUseCaseTest`/`SaveBudgetUseCaseTest` if present) to `Long` spend/limit and assert `percentage` is still a correct fractional ratio.

### Import
- [X] T049 [US5] `domain/import/.../usecase/ParseStatementUseCase.kt:44,733,736` — the AI-response DTO `@SerialName("a") amount: Double` (line 44) **stays Double** (transient wire value, like `AmountParser`); convert once via `majorToMinor(tx.amount)` and use that `Long` both for `generateTransactionHash(date, <minor>, …)` (line 733, see T031) and for `ParsedTransaction(amount = <minor>)` (line 736). `AmountParser.parseAmount` itself stays `Double` (research R4).
- [X] T050 [US5] `domain/import/.../usecase/ImportTransactionsUseCase.kt:86,90,115-116` — `amount` and balance delta on `Long`; `updateBalance(_, delta: Long, _)`.
- [X] T051 [US5] Update `domain/import/src/test/.../ImportTransactionsUseCaseTest.kt` — `parsedTx(amount: Long = 10000L)`; `coVerify { updateBalance(7L, -10000L, any()) }`, etc.

### Exchange rate
- [X] T052 [US5] `domain/exchangerate/.../usecase/ConvertAmountUseCase.kt:30-47` — new signature `invoke(amountMinor: Long, source, target, quotes: Map<String,Double>): Long`; algorithm per research R3 (`BigDecimal(amountMinor).movePointLeft(2)` → ×sourceRate ÷ targetRate at scale 10 HALF_UP → `movePointRight(2).setScale(0, HALF_UP).toLong()`); same-currency short-circuit returns `amountMinor`. Keep `quotes` Double.
- [X] T053 [US5] Rewrite `domain/exchangerate/src/test/.../ConvertAmountUseCaseTest.kt` (9 tests) — assert `Long` minor (e.g. KZT→USD `105.26` → `10526`; USD→KZT `47_500.0` → `4_750_000`; HALF_UP `2.35` → `235`; same-currency passthrough; cross-currency pivot; missing-quote `IllegalArgumentException`).
- [X] T054 [US5] Build gate: `./gradlew :domain:statistics:test :domain:transactions:test :domain:budgets:test :domain:import:test :domain:exchangerate:test` green.

---

## Phase 7: Sync — `core:firestore` DTOs + `data:sync` (US6)

### Goal: Dual-write `*Minor` encrypted field + legacy field; read `*Minor`-first; preserve all sync invariants.

### Independent test criteria:
- DTO mapper tests pass: dual-field write, `*Minor`-first read, legacy `Double` doc back-compat, plaintext v0 fallback.
- Sync invariants (owner-guard, anon-skip, push-before-persist, delete idempotency, dedup) unchanged.

### DTO fields (`core/firestore/.../dto/`)
- [X] T055 [P] [US6] `TransactionDto.kt` — add `amountMinor: String? = null`.
- [X] T056 [P] [US6] `AccountDto.kt` — add `balanceMinor: String? = null`.
- [X] T057 [P] [US6] `BudgetDto.kt` — add `monthlyLimitMinor: String? = null`.
- [X] T058 [P] [US6] `DebtDto.kt` — add `totalAmountMinor: String? = null`.
- [X] T059 [P] [US6] `DebtPaymentDto.kt` — add `amountMinor: String? = null`.
- [X] T060 [P] [US6] `RecurringTransactionDto.kt` — add `amountMinor: String? = null`.

### Push (entity→dto) dual-write — `core/firestore/.../mapper/`
- [X] T061 [US6] `TransactionDtoMapper.kt:19` — legacy `amount = encryptDouble(amount.toMajorDouble())`/plaintext `amount.toMajorPlainString()`; new `amountMinor = encryptLong(amount)`/plaintext `amount.toString()`.
- [X] T062 [US6] Apply the same dual-write to `AccountDtoMapper.kt:17` (`balance`/`balanceMinor`), `BudgetDtoMapper.kt:19`, `DebtDtoMapper.kt:20`, `DebtPaymentDtoMapper.kt:20`, `RecurringTransactionDtoMapper.kt:19`.

### Pull (dto→entity) read-preference — same mapper files
- [X] T063 [US6] `TransactionDtoMapper.kt:49,64` — `amount = amountMinor?.takeIf{it.isNotBlank()}?.let{ cipher.decryptLong(it) } ?: majorToMinor(cipher.decryptDouble(amount))` in the encrypted branch; mirror in the plaintext v0 branch (`amountMinor?.toLongOrNull() ?: majorToMinor(amount.toDouble())`).
- [X] T064 [US6] Apply the same `*Minor`-first read + legacy fallback to `AccountDtoMapper.kt:38,49`, `BudgetDtoMapper.kt:43,52-60`, `DebtDtoMapper.kt:49,60-64`, `DebtPaymentDtoMapper.kt:48,60-63`, `RecurringTransactionDtoMapper.kt:62,82-85`.

### data:sync
- [X] T065 [US6] `data/sync/.../PullSyncUseCase.kt:252-254` — balance revert delta `if (type=="income") -amount else amount` on `Long`; keep `!local.isDeleted` idempotency guard.

### Sync tests (`core/firestore/src/test/.../mapper/`)
- [X] T066 [P] [US6] Update `TransactionDtoMapperTest.kt` — push writes both `amount` and `amountMinor`; pull prefers `amountMinor` (`10526` etc.); legacy doc with only `amount` ("12345.67") back-compat → `1234567`; plaintext v0 fallback.
- [X] T067 [P] [US6] Update `AccountDtoMapperTest.kt` (and add coverage for budget/debt/debtPayment/recurring DTO mappers) — same dual-write + read-preference + legacy fallback assertions.
- [X] T068 [US6] Add a forward-compat assertion: an old-shaped object (no `*Minor`) deserializes and reads correctly (Firestore ignores unknown fields on old clients — assert our reader's legacy path).
- [X] T069 [US6] Build gate: `./gradlew :core:firestore:test :data:sync:test` green.

---

## Phase 8: `presentation:*` + `core:ui` (US7)

### Goal: Parse input → minor at existing choke points; format minor → major at the UI edge; State on Long.

### Independent test criteria:
- All `presentation:*` compile; updated VM tests green; screenshots unchanged (or re-recorded).

### core:ui (`core/ui/src/main/java/com/atelbay/money_manager/core/ui/`)
- [X] T070 [US7] `util/MoneyDisplayFormatter.kt:72-85` — `formatAmount(amountMinor: Long, sign, formatter)` divides by `100.0` internally (rendered string identical).
- [X] T071 [P] [US7] `components/BalanceCard.kt:61,150` — `balance: Long`; animate `balance.toMajorDouble()`.
- [X] T072 [P] [US7] `components/IncomeExpenseCard.kt:43-44,53,61` — `income: Long`, `expense: Long`; `net = income - expense` (Long); `savingsRate = (net.toMajorDouble()/income.toMajorDouble()).toFloat()`.
- [X] T073 [P] [US7] `components/TransactionListItem.kt:55,60,71` — `amount: Long`, `secondaryAmount: Long?`.
- [X] T074 [P] [US7] `components/SummaryStatCard.kt:46,164` — `value: Long?`.
- [X] T075 [P] [US7] `components/AccountCard.kt:33,92` — `balance: Long`; inline formatter divides by 100.

### Input parse sites (replace `toDoubleOrNull()` → `parseToMinorUnitsOrNull()`; pre-fill → `toMajorPlainString()`)
- [X] T076 [US7] `presentation/transactions/.../edit/TransactionEditViewModel.kt:127-141,189,74` — sanitize+parse to `Long?`; `save` validates `> 0L`; pre-fill via `toMajorPlainString()`.
- [X] T077 [P] [US7] `presentation/onboarding/.../CreateAccountViewModel.kt:56,71-79` — parse initial balance to `Long` (empty → `0L`).
- [X] T078 [P] [US7] `presentation/budgets/.../edit/BudgetEditViewModel.kt:87,108,66` — limit parse to `Long`; pre-fill.
- [X] T079 [P] [US7] `presentation/debts/.../list/DebtEditBottomSheet.kt:64,203,219` — parse `totalAmount` to `Long`; pre-fill.
- [X] T080 [P] [US7] `presentation/debts/.../detail/PaymentBottomSheet.kt:48,54,142,147` and `DebtDetailViewModel.addPayment(amount: Long)` — parse payment to `Long`.
- [X] T081 [P] [US7] `presentation/recurring/.../edit/RecurringEditViewModel.kt:101,177,63` — amount parse to `Long`; pre-fill.
- [X] T082 [P] [US7] `presentation/import/.../components/ParsedTransactionItem.kt:112-125` — display `Long` minor via `toMajorPlainString()`; parse edits to `Long`.

### Presentation State (Double → Long)
- [X] T083 [P] [US7] `presentation/transactions/.../list/TransactionListState.kt:13,20,21,25,36-38,44-46` — `balance`/`periodIncome`/`periodExpense` `Long?`; `dailyNetSums: Map<String, Long>`; `TransactionRowState.originalAmount`/`convertedAmount`/`displayAmount` `Long`.
- [X] T084 [P] [US7] `presentation/accounts/.../list/AccountListState.kt:10` (`totalBalance: Long`) and `.../edit/AccountEditState.kt:14` (`originalBalance: Long`).
- [X] T085 [P] [US7] `presentation/debts/.../list/DebtListState.kt:11-12` — `totalLent`, `totalBorrowed` `Long`.
- [X] T086 [P] [US7] `presentation/statistics/.../StatisticsState.kt:16,28,34,41,48-57` and `CategoryTransactionsState.kt:37` — chart/category/daily/monthly/total fields `Long`/`Long?`.

### ViewModels / resolvers (compute on Long; charts via `toMajorDouble()`)
- [X] T087 [US7] `presentation/transactions/.../list/TransactionListViewModel.kt:309,331-365,419-470,531-535` — `convertAmountUseCase(amountMinor: Long): Long`; sums on `Long`; row states `Long`.
- [X] T088 [US7] `presentation/transactions/.../list/TransactionListScreen.kt:198,278-280,409-414,457-461` — pass `Long` to `BalanceCard`/`IncomeExpenseCard`/`TransactionListItem`; `formatAmount(abs(dailyNet))` on `Long`.
- [X] T089 [US7] `presentation/statistics/.../StatisticsCurrencyDisplayResolver.kt:101-121` — conversion + `sumOf` on `Long`; resolution fields `Long?`.
- [X] T090 [US7] `presentation/statistics/.../StatisticsViewModel.kt:245-255,295-325` — build chart points feeding Vico via `amount.toMajorDouble()` (never store Double).
- [X] T091 [US7] `presentation/statistics/.../StatisticsScreen.kt:361,422-440,888,1258` and `CategoryTransactionsViewModel.kt:75` — pass `Long`/format on `Long`.

### Local screen formatters (param `Double` → `Long`, divide by 100; keep K/M behavior)
- [X] T092 [P] [US7] `presentation/accounts/.../list/AccountListScreen.kt:117,231,300-301`.
- [X] T093 [P] [US7] `presentation/budgets/.../list/BudgetListScreen.kt:225,267,276-284`.
- [X] T094 [P] [US7] `presentation/debts/.../list/DebtListScreen.kt:263,277,375,415,475`.
- [X] T095 [P] [US7] `presentation/debts/.../detail/DebtDetailScreen.kt:246,316-317,460,525-533`.
- [X] T096 [P] [US7] `presentation/recurring/.../list/RecurringListScreen.kt:239,243` — `formatAmount(recurring.amount: Long, …)`.

### Presentation tests
- [X] T097 [P] [US7] Update `presentation/transactions/src/test/.../TransactionListViewModelTest.kt` — `Long` assertions (e.g. `convertedAmount == 10000L`, `balance == 12000L`).
- [X] T098 [P] [US7] Update `presentation/statistics/src/test/.../StatisticsCurrencyDisplayResolverTest.kt` — `Long` assertions (e.g. `4_750_000L`).
- [X] T099 [US7] Re-record Roborazzi screenshots only if pixels shift: run `./gradlew :app:recordRoborazziDebug` for `CategoryBottomSheetScreenshotTest` + `TransactionEditScreenScreenshotTest`, then `git diff --stat` the PNGs and commit only real diffs (expected identical).
- [X] T100 [US7] Build gate: `./gradlew assembleDebug` green.

---

## Phase 9: Polish & Verification (US8)

- [X] T101 [US8] Run full suite: `./gradlew test` and `./gradlew :core:database:connectedAndroidTest` — all green (SC-001).
- [X] T102 [US8] Grep gate (SC-002): confirm no `Double`-typed money field remains in `domain/`, `data/`, `core/model` except `ExchangeRate.quotes` (and the documented transients `AmountParser.parseAmount` + `ParseStatementUseCase` AI DTO): `grep -rn "Double" core/model/src domain/*/src/main data/*/src/main | grep -iE "amount|balance|limit|total|paid|remaining|spent"` returns only the documented exceptions.
- [X] T103 [US8] Add penny-drift regression test (SC-004) — apply a series of fractional transactions (e.g. repeated `0.1`/`0.2`-style minor values) through the balance path and assert zero drift vs `SUM`; assert FR-013 `balance == SUM(signed amounts)` per account.
- [X] T104 [US8] Document the ×100 fixed scale decision in `AGENTS.md` (Critical rules / a Money section) and add a project memory entry (`project_money_minor_units.md` + MEMORY.md pointer).
- [ ] T105 [US8] Manual verification (SC-008) against a real Firestore test account: pull legacy `Double` docs (→ correct minor), push dual-write (verify both `amount` and `*Minor` fields written), read back (prefers `*Minor`); confirm balances/integrity. Record results in the PR.
- [X] T106 [US8] Final review against SC-001…SC-008 and the non-regression invariants (signs, `createdAt`, `GetCategoriesUseCase`, soft-delete sync).

---

## Dependencies

```
US1 (core:model) ─┬─> US2 (core:database) ─┐
                  ├─> US3 (core:common)  ──┼─> US4 (data) ─┬─> US5 (domain) ─> US7 (presentation) ─> US8
                  │                        │               └─> US6 (sync) ────────────────────────┘
                  └────────────────────────┘
```
- US2 and US3 can proceed in parallel after US1.
- US5 and US6 can proceed in parallel after US4 (US6 also needs US3's hashing).
- US5 itself spans `domain:statistics`, `domain:transactions` (repo contract), `domain:budgets`,
  `domain:import`, `domain:exchangerate` — T045 is a cross-layer interface/impl pair, do it before
  the US5 build gate (T054).
- US7 needs US5; US8 needs everything.

## Parallel execution examples

- **US1**: T004–T011 (`[P]`) — independent model files — run together; then T003 helper tests.
- **US2**: T013–T018 (`[P]`) entity files together; migration tasks T022–T025 are sequential.
- **US5**: T043 (`[P]`) and T048 (`[P]`) parallel; T045 (repo interface+impl) and T046/T047
  (budgets) are sequential within their files.
- **US6**: T055–T060 (`[P]`) DTO field additions together; then push (T061–T062) → pull (T063–T064).
- **US7**: T071–T075 core:ui components `[P]`; T077–T082 input sites `[P]`; T092–T096 formatters `[P]`.

## Implementation strategy

This migration is **all-or-nothing per build** — it cannot ship partially. Execute workstreams
strictly in dependency order, keeping each module green before moving on. The natural review
checkpoints are the build gates (T012, T029, T033, T042, T054, T069, T100, T101). The
highest-risk tasks are **T023/T026** (migration correctness), **T030/T032** (hash stability),
and **T061–T068** (sync compat) — review these most carefully and do not merge before T105's
manual Firestore check.

---

## Summary

- **Total tasks**: 106
- **Per workstream**: US1=11, US2=17, US3=4, US4=9, US5=12, US6=15, US7=31, US8=6, Setup=1
- **Test tasks**: T003, T026–T028, T032, T041, T048, T051, T053, T066–T068, T097–T099, T103 (+ build gates)
- **Parallel opportunities**: model fields (US1), entities (US2), DTO fields + tests (US6), UI components/input sites/formatters (US7)
- **Highest-risk**: T023/T026 (migration), T030/T032 (uniqueHash), T061–T068 (sync compat)
- **Cannot be an MVP subset** — the app does not compile half-migrated; deliver all workstreams together on this branch behind the build gates.
