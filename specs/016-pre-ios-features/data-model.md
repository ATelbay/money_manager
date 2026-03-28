# Data Model: Pre-iOS Port Feature Bundle

**Branch**: `016-pre-ios-features` | **Date**: 2026-03-27

## Entity: RecurringTransactionEntity

**Table**: `recurring_transactions`
**Location**: `core/database/entity/RecurringTransactionEntity.kt`

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | Long | PK, autoGenerate | |
| amount | Double | NOT NULL | Positive value |
| type | String | NOT NULL | "income" or "expense" |
| categoryId | Long | FK → categories(id), CASCADE | |
| accountId | Long | FK → accounts(id), CASCADE | |
| note | String? | nullable | |
| frequency | String | NOT NULL | "DAILY", "WEEKLY", "MONTHLY", "YEARLY" |
| startDate | Long | NOT NULL | Epoch millis |
| endDate | Long? | nullable | null = no end date |
| dayOfMonth | Int? | nullable | 1–31 for MONTHLY; overflow clamped to last day of month |
| dayOfWeek | Int? | nullable | 1=Monday..7=Sunday for WEEKLY |
| lastGeneratedDate | Long? | nullable | Epoch millis, null = never generated |
| isActive | Boolean | NOT NULL, default true | |
| createdAt | Long | NOT NULL | Epoch millis |
| remoteId | String? | nullable | For future Firestore sync |
| updatedAt | Long | NOT NULL, default 0 | |
| isDeleted | Boolean | NOT NULL, default false | Soft delete |

**Indices**:
- `idx_recurring_account` on `accountId`
- `idx_recurring_category` on `categoryId`
- `idx_recurring_active` on `isActive, isDeleted`

**Foreign Keys**:
- `categoryId` → `categories(id)` ON DELETE CASCADE
- `accountId` → `accounts(id)` ON DELETE CASCADE

---

## Entity: BudgetEntity

**Table**: `budgets`
**Location**: `core/database/entity/BudgetEntity.kt`

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | Long | PK, autoGenerate | |
| categoryId | Long | FK → categories(id), CASCADE, UNIQUE | One budget per category |
| monthlyLimit | Double | NOT NULL | Positive value |
| createdAt | Long | NOT NULL | Epoch millis |
| remoteId | String? | nullable | For future Firestore sync |
| updatedAt | Long | NOT NULL, default 0 | |
| isDeleted | Boolean | NOT NULL, default false | Soft delete |

**Indices**:
- `idx_budget_category` on `categoryId` (unique)

**Foreign Keys**:
- `categoryId` → `categories(id)` ON DELETE CASCADE

---

## Domain Model: RecurringTransaction

**Location**: `core/model/RecurringTransaction.kt`

```
data class RecurringTransaction(
    val id: Long = 0,
    val amount: Double,
    val type: TransactionType,
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: Long,
    val accountId: Long,
    val accountName: String,
    val note: String?,
    val frequency: Frequency,
    val startDate: Long,
    val endDate: Long?,
    val dayOfMonth: Int?,
    val dayOfWeek: Int?,
    val lastGeneratedDate: Long?,
    val isActive: Boolean,
    val createdAt: Long,
)
```

Note: Domain model is enriched with denormalized category/account names. The category fields (categoryName, categoryIcon, categoryColor) follow the same pattern as `Transaction`. The `accountName` field is specific to RecurringTransaction — the base `Transaction` model has only `accountId` without a denormalized account name. Sync fields (`remoteId`, `updatedAt`, `isDeleted`) are excluded from the domain model.

---

## Domain Model: Budget

**Location**: `core/model/Budget.kt`

```
data class Budget(
    val id: Long = 0,
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: Long,
    val monthlyLimit: Double,
    val spent: Double,
    val remaining: Double,
    val percentage: Float,
)
```

Note: `spent`, `remaining`, and `percentage` are computed fields — derived by `GetBudgetsWithSpendingUseCase` from transaction data, not stored in the entity.

---

## Enum: Frequency

**Location**: `core/model/Frequency.kt`

```
enum class Frequency {
    DAILY, WEEKLY, MONTHLY, YEARLY
}
```

Used in domain model. Mapped to/from String in the entity via mapper.

---

## Updated Enum: Period

**Location**: `presentation/transactions/.../TransactionListState.kt`

```
enum class Period {
    ALL,            // Retained for programmatic use, no chip
    CURRENT_MONTH,  // NEW — 1st of month → today (default)
    TODAY,
    WEEK,
    MONTH,          // Renamed label to "30 days" in AppStrings
    YEAR,
}
```

Default changes from `Period.MONTH` to `Period.CURRENT_MONTH`.

---

## Updated State: TransactionListState

New fields added:

```
val customDateRange: Pair<Long, Long>? = null,    // start/end millis
val showDatePickerDialog: Boolean = false,
val customDateLabel: String? = null,               // e.g., "Февраль 2026" or "5 мар – 20 мар"
```

When `customDateRange != null`, the ViewModel ignores `selectedPeriod` for filtering.

---

## New DAO Query: TransactionDao Extension

```
@Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE categoryId = :categoryId AND type = 'expense' AND isDeleted = 0 AND date >= :startDate AND date < :endDate")
fun observeExpenseSumByCategory(categoryId: Long, startDate: Long, endDate: Long): Flow<Double>
```

Used by `GetBudgetsWithSpendingUseCase` to efficiently compute per-category spending for the current month.

---

## Migration: 4 → 5

**File**: `core/database/migration/Migration_4_5.kt`

Two CREATE TABLE statements:

1. `CREATE TABLE IF NOT EXISTS recurring_transactions (...)` — all columns as defined above
2. `CREATE TABLE IF NOT EXISTS budgets (...)` — all columns as defined above
3. `CREATE INDEX IF NOT EXISTS idx_recurring_account ON recurring_transactions(accountId)`
4. `CREATE INDEX IF NOT EXISTS idx_recurring_category ON recurring_transactions(categoryId)`
5. `CREATE INDEX IF NOT EXISTS idx_recurring_active ON recurring_transactions(isActive, isDeleted)`
6. `CREATE UNIQUE INDEX IF NOT EXISTS idx_budget_category ON budgets(categoryId)`

---

## State Transitions

### RecurringTransaction Lifecycle

```
Created (isActive=true, lastGeneratedDate=null)
  → Generating: on app startup, compute missed dates, create transactions
  → Generated (lastGeneratedDate updated)
  → Paused (isActive=false, user toggle)
  → Resumed (isActive=true, user toggle)
  → Expired (isActive=false, endDate passed, set by generation logic)
  → Deleted (isDeleted=true, soft delete)
```

### Budget (no state machine — purely declarative)

```
Created → Active (spending calculated on read) → Deleted (soft delete)
```

Budget has no lifecycle transitions — spending is computed fresh on every read from transaction data.
