# Data Model: Money as Integer Minor Units

**Spec**: [spec.md](./spec.md) · **Research**: [research.md](./research.md) · **Date**: 2026-06-08

All money is `Long` minor units at **fixed scale 2** (hundredths). Exchange-rate `quotes` stay
`Double`. `percentage`/`savingsRate`/ratios stay `Float`/`Double`.

---

## Conversion helpers (NEW — `core:model`)

New file `core/model/.../money/MoneyConversions.kt`:

```kotlin
const val MINOR_UNIT_SCALE = 2
private val SCALE_FACTOR = java.math.BigDecimal.TEN.pow(MINOR_UNIT_SCALE) // 100
const val MAX_MAJOR_UNITS = 9.2e16 // Long.MAX / 100, documented ceiling

fun BigDecimal.toMinorUnits(): Long                 // setScale(2, HALF_UP).movePointRight(2).longValueExact(), overflow-guarded
fun majorToMinor(major: Double): Long               // BigDecimal(major.toString()).toMinorUnits()
fun majorToMinor(major: BigDecimal): Long
fun String.parseToMinorUnitsOrNull(): Long?         // sanitized BigDecimal parse → toMinorUnits(); null on invalid
fun Long.toMajor(): BigDecimal                       // BigDecimal(this).movePointLeft(2)
fun Long.toMajorDouble(): Double                     // this / 100.0  (display/animation only)
fun Long.toMajorPlainString(): String                // toMajor().stripTrailingZeros().toPlainString()  (edit pre-fill)
```

Rounding is `HALF_UP` at every boundary. `toMinorUnits` throws `ArithmeticException` on overflow.

---

## Domain models (`core:model`) — field type changes

| Model | Field | Before | After |
|-------|-------|--------|-------|
| `Transaction` | `amount` | `Double` | `Long` (positive magnitude; sign via `type`) |
| `Account` | `balance` | `Double` | `Long` |
| `Account` | `createdAt` | `Long = 0` | unchanged (not money) |
| `Budget` | `monthlyLimit` | `Double` | `Long` |
| `Budget` | `spent` | `Double` | `Long` |
| `Budget` | `remaining` | `Double` | `Long` |
| `Budget` | `percentage` | `Float` | unchanged (ratio) |
| `Debt` | `totalAmount` | `Double` | `Long` |
| `Debt` | `paidAmount` | `Double = 0.0` | `Long = 0L` |
| `Debt` | `remainingAmount` | `Double` | `Long` |
| `DebtPayment` | `amount` | `Double` | `Long` |
| `RecurringTransaction` | `amount` | `Double` | `Long` |
| `ParsedTransaction` | `amount` | `Double` | `Long` |
| `TransactionOverride` | `amount` | `Double? = null` | `Long? = null` |
| `ExchangeRate` | `quotes` | `Map<String,Double>` | unchanged (coefficients) |

---

## Room entities (`core:database`) — REAL → INTEGER

Schema version **8 → 9** (`MoneyManagerDatabase.kt:33`, `exportSchema = true`). New `9.json`
committed. Columns (all NOT NULL, no default, stored positive except `balance`):

| Table | Column | Entity field |
|-------|--------|--------------|
| `transactions` | `amount` | `TransactionEntity.amount` (`:35`) |
| `accounts` | `balance` | `AccountEntity.balance` (`:18`) |
| `budgets` | `monthlyLimit` | `BudgetEntity.monthlyLimit` (`:26`) |
| `debts` | `totalAmount` | `DebtEntity.totalAmount` (`:27`) |
| `debt_payments` | `amount` | `DebtPaymentEntity.amount` (`:33`) |
| `recurring_transactions` | `amount` | `RecurringTransactionEntity.amount` (`:33`) |

### DAO signature changes
- `AccountDao.updateBalance(accountId, delta: Long, updatedAt)` (`AccountDao.kt:34`).
- `TransactionDao.observeExpenseSumByCategory(...): Flow<Long>` with `COALESCE(SUM(amount), 0)`
  (`TransactionDao.kt:130`).
- `DebtPaymentDao.sumAmountByDebtId(...): Flow<Long?>` (`DebtPaymentDao.kt:27`).

---

## Migration_8_9

New `migration/Migration_8_9.kt`, registered in `DatabaseModule.kt:68` `.addMigrations(...)`.
SQLite has no `ALTER COLUMN`, so each of the 6 tables uses the rename → create-new(INTEGER) →
`INSERT … SELECT CAST(ROUND(col * 100.0) AS INTEGER)` → drop-old → rename pattern, **recreating
indexes/foreign keys** (notably the `transactions.uniqueHash` unique index and account FKs).

`uniqueHash` values are copied **as-is** (not recomputed) — see research R1. `updatedAt`/sync
columns are preserved unchanged so sync state is not disturbed.

Conversion expression per money column: `CAST(ROUND(<col> * 100.0) AS INTEGER)` (≡ HALF_UP).

---

## Firestore DTOs (`core:firestore`) — dual-write fields

Each money-bearing DTO gains a nullable encrypted `*Minor` field beside the legacy field:

| DTO | Legacy field (keep) | New field |
|-----|---------------------|-----------|
| `TransactionDto` | `amount: String` | `amountMinor: String? = null` |
| `AccountDto` | `balance: String` | `balanceMinor: String? = null` |
| `BudgetDto` | `monthlyLimit: String` | `monthlyLimitMinor: String? = null` |
| `DebtDto` | `totalAmount: String` | `totalAmountMinor: String? = null` |
| `DebtPaymentDto` | `amount: String` | `amountMinor: String? = null` |
| `RecurringTransactionDto` | `amount: String` | `amountMinor: String? = null` |

- **Push** (`*DtoMapper` entity→dto): legacy = `encryptDouble(minor.toMajorDouble())` (or
  plaintext `minor.toMajorPlainString()`); new = `encryptLong(minor)` (or plaintext
  `minor.toString()`).
- **Pull** (dto→entity): `val minor = amountMinor?.takeIf{it.isNotBlank()}?.let{decryptLong(it)}
  ?: majorToMinor(decryptDouble(amount))` (with the same plaintext-fallback branch the mappers
  already have for `encryptionVersion == 0`).

`FieldCipher.encryptLong`/`decryptLong` already exist — no crypto changes.

---

## Hashing (`core:common`)

`generateTransactionHash(date, amountMinor: Long, type, details)`: internally
`val amount = amountMinor / 100.0` then the **unchanged** `"$date|$amount|$type|${details.take(30)}"`
SHA-256, preserving cross-version digest equality (research R1). All call sites updated to pass
`Long`.

---

## Statistics (`domain:statistics`) — model & compute

`StatisticsModels.kt`: `CategorySummary.totalAmount`, `DailyTotal.amount`, `MonthlyTotal.amount`,
`PeriodSummary.totalExpenses`, `PeriodSummary.totalIncome` → `Long`. `GetPeriodSummaryUseCase`
`.sumOf { it.amount }` operates on `Long` (use `sumOf { it.amount }` returning `Long`).

---

## Presentation State — field type changes (`Double` → `Long`)

| File | Field(s) |
|------|----------|
| `transactions/.../TransactionListState.kt` | `balance`, `periodIncome`, `periodExpense` (`Long?`); `dailyNetSums: Map<String, Long>`; `TransactionRowState.originalAmount`/`convertedAmount`/`displayAmount` (`Long`) |
| `accounts/.../AccountListState.kt` | `totalBalance: Long` |
| `accounts/.../AccountEditState.kt` | `originalBalance: Long` |
| `debts/.../DebtListState.kt` | `totalLent`, `totalBorrowed` (`Long`) |
| `statistics/.../StatisticsState.kt` | `StatisticsChartPoint.amount` (`Long?` → chart consumes via `toMajorDouble()`), `StatisticsCategoryDisplayItem.displayAmount`, `StatisticsDisplayDailyTotal.amount`, `StatisticsDisplayMonthlyTotal.amount`, `totalExpenses`, `totalIncome`, `displayedTotalExpenses`, `displayedTotalIncome` |
| `statistics/.../CategoryTransactionsState.kt` | `CategoryTransactionItem.amount: Long` |
| `statistics/.../StatisticsCurrencyDisplayResolver.kt` | `StatisticsCurrencyResolution.displayedTotalExpenses`/`displayedTotalIncome` |

Vico charts need `Number` — feed `amount.toMajorDouble()` at the chart-point build site
(`StatisticsViewModel.kt:245-255`), not stored as `Double`.

---

## `core:ui` formatting — accept `Long` minor

- `MoneyDisplayPresentation.formatAmount(amountMinor: Long, sign, formatter)` — divides by
  `100.0` internally; rendered 2-dp string identical to today.
- `BalanceCard(balance: Long, …)` — `Animatable` animates `balance.toMajorDouble()`.
- `IncomeExpenseCard(income: Long, expense: Long, …)` — `net = income - expense` (Long);
  `savingsRate = (net.toMajorDouble() / income.toMajorDouble()).toFloat()`.
- `TransactionListItem(amount: Long, secondaryAmount: Long?, …)`.
- `SummaryStatCard(value: Long?, …)`.
- `AccountCard(balance: Long, …)` — inline formatter divides by 100.
- Local screen helpers (`AccountListScreen`, `BudgetListScreen`, `DebtListScreen`,
  `DebtDetailScreen`) — param `Long`, divide by 100 before existing K/M abbreviation.

---

## Validation rules (unchanged semantics, new type)

- Parsed amount must be `> 0L` (forms reject `null`/`<= 0`).
- `transactions.amount` stored positive; sign applied via `type` at balance time.
- `Debt.remainingAmount = (totalAmount - paidAmount).coerceAtLeast(0L)` (`DebtMapper.kt:9`).
- `Account.createdAt` default `0`, real timestamp at `AccountRepositoryImpl.kt:35`.
- FR-013: `accounts.balance == SUM(signed transaction amounts)` per account (test-asserted).
