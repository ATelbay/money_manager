# UI Contract: Statistics Screen

## Screen States

### Loading State
- Period selector visible and interactive
- Centered `CircularProgressIndicator`
- No charts or data visible

### Error State
- Period selector visible and interactive
- Transaction type toggle visible
- Error icon + message centered
- "Retry" button below message
- testTag: `statistics:error`, `statistics:retryButton`

### Empty State
- Period selector visible and interactive
- Transaction type toggle visible
- Bar chart icon + "Нет данных за период" message
- testTag: `statistics:emptyState`

### Loaded State (Expenses)
- Period selector (WEEK/MONTH/YEAR chips)
- Transaction type toggle (Расходы ● / Доходы)
- Summary cards row: Expenses + Income totals
- Donut chart with expense categories + legend
- Bar chart:
  - WEEK: 7 daily bars, labels = day-of-week abbreviation
  - MONTH: 30 daily bars, labels = day number (every 5th bar)
  - YEAR: 12 monthly bars, labels = month abbreviation
- Category breakdown list: color dot, name, percentage badge, amount

### Loaded State (Income)
- Same layout as Expenses
- Donut chart shows income categories
- Bar chart shows daily/monthly income
- Category breakdown shows income categories
- Center text shows "Доходы" + total income amount

## Composable Signatures

```
StatisticsScreen(
    state: StatisticsState,
    onPeriodChange: (StatsPeriod) -> Unit,
    onTransactionTypeChange: (TransactionType) -> Unit,  // NEW
    onRetry: () -> Unit,                                  // NEW
    modifier: Modifier = Modifier,
)

StatisticsRoute(
    modifier: Modifier = Modifier,
    viewModel: StatisticsViewModel = hiltViewModel(),
)
```

## ViewModel Public API

```
class StatisticsViewModel {
    val state: StateFlow<StatisticsState>
    fun setPeriod(period: StatsPeriod)
    fun setTransactionType(type: TransactionType)  // NEW
    fun retry()                                     // NEW
}
```

## Test Tags

| Tag | Component |
|-----|-----------|
| `statistics:screen` | Scaffold root |
| `statistics:content` | LazyColumn |
| `statistics:periodSelector` | Period chip row |
| `statistics:period_WEEK` | Week chip |
| `statistics:period_MONTH` | Month chip |
| `statistics:period_YEAR` | Year chip |
| `statistics:typeToggle` | Expense/Income segmented button |
| `statistics:totalExpenses` | Expense summary card |
| `statistics:totalIncome` | Income summary card |
| `statistics:pieChart` | Donut chart card |
| `statistics:barChart` | Bar chart canvas |
| `statistics:category_{id}` | Category breakdown row |
| `statistics:error` | Error state container |
| `statistics:retryButton` | Retry button |
| `statistics:emptyState` | Empty state container |
