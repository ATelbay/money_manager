# Data Model: Statistics Screen Audit

## Modified Entities

### StatsPeriod (unchanged)

```
enum: WEEK, MONTH, YEAR
```

No changes.

### TransactionType (NEW — in domain:statistics models)

```
enum: EXPENSE, INCOME
```

Used for the expense/income toggle. Not a database entity — exists only in the statistics domain model.

### CategorySummary (MODIFIED)

```
categoryId: Long
categoryName: String
categoryIcon: String
categoryColor: Long
totalAmount: Double
percentage: Int          ← CHANGED from Float to Int (pre-rounded via largest-remainder)
```

**Change**: `percentage` becomes `Int` (was `Float`). Pre-computed in UseCase using largest-remainder method. Sum guaranteed to equal 100.

### DailyTotal (unchanged)

```
date: Long     # millis at 00:00:00.000 of the day
amount: Double
```

Now guaranteed to include zero-amount entries for all days in the period (WEEK/MONTH).

### MonthlyTotal (NEW)

```
year: Int       # e.g., 2026
month: Int      # 0-indexed (Calendar.MONTH)
amount: Double
label: String   # pre-formatted month abbreviation (e.g., "Мар")
```

Used for YEAR period aggregation. 12 entries, one per calendar month.

### PeriodSummary (MODIFIED)

```
totalExpenses: Double
totalIncome: Double
expensesByCategory: List<CategorySummary>
incomesByCategory: List<CategorySummary>    ← NEW
dailyExpenses: List<DailyTotal>
dailyIncome: List<DailyTotal>               ← NEW
monthlyExpenses: List<MonthlyTotal>         ← NEW (for YEAR period)
monthlyIncome: List<MonthlyTotal>           ← NEW (for YEAR period)
```

### StatisticsState (MODIFIED — presentation layer)

```
period: StatsPeriod = MONTH
transactionType: TransactionType = EXPENSE   ← NEW
totalExpenses: Double = 0.0
totalIncome: Double = 0.0
expensesByCategory: ImmutableList<CategorySummary>
incomesByCategory: ImmutableList<CategorySummary>    ← NEW
dailyExpenses: ImmutableList<DailyTotal>
dailyIncome: ImmutableList<DailyTotal>               ← NEW
monthlyExpenses: ImmutableList<MonthlyTotal>          ← NEW
monthlyIncome: ImmutableList<MonthlyTotal>            ← NEW
isLoading: Boolean = true
error: String? = null                                 ← NEW
```

## Data Flow

```
TransactionDao.observeByDateRange(start, end)
  + CategoryDao.observeAll()
  → combine → GetPeriodSummaryUseCase
    → filter by type (expense/income)
    → group by category → percentage (largest-remainder)
    → group by day/month → fill zeros
    → PeriodSummary
  → StatisticsViewModel (.catch for errors, .distinctUntilChanged)
    → StatisticsState
  → StatisticsScreen (reads transactionType to select which data to display)
```

## Validation Rules

- `CategorySummary.percentage`: sum of all percentages in a list MUST equal 100 (when list is non-empty)
- `CategorySummary.percentage`: minimum 1% for any non-zero category
- `DailyTotal` list: MUST contain exactly N entries (7 for WEEK, 30 for MONTH), sorted by date ascending
- `MonthlyTotal` list: MUST contain exactly 12 entries for YEAR, sorted chronologically
- `PeriodSummary.totalExpenses`: MUST equal sum of all `expensesByCategory[*].totalAmount`
- `StatisticsState.error`: when non-null, `isLoading` MUST be false

## State Transitions

```
Initial → Loading (isLoading=true, error=null)
Loading → Loaded (isLoading=false, error=null, data populated)
Loading → Error (isLoading=false, error="message")
Error → Loading (onRetry → isLoading=true, error=null)
Loaded → Loading (period change or type toggle)
```
