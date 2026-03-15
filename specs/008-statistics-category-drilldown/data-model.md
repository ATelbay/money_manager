# Data Model: Statistics UX Cleanup + Category Transaction Drill-Down

**Branch**: `008-statistics-category-drilldown` | **Date**: 2026-03-15

## New Entity: StatisticsDateRange

**Module**: `domain:statistics`  
**Purpose**: Shared inclusive time window used by Statistics summary generation and preserved for drill-down.

| Attribute | Type | Description |
|-----------|------|-------------|
| startMillis | Long | Inclusive range start used for the currently selected Statistics period |
| endMillis | Long | Inclusive range end used for the currently selected Statistics period |

**Notes**:

- Produced by a shared `StatisticsPeriodRangeResolver`
- Stored in summary/state so drill-down can use the exact same resolved range later

## Modified Entity: PeriodSummary

**Module**: `domain:statistics`  
**Purpose**: Existing Statistics aggregate result enriched with the resolved date range it was computed from.

| Attribute | Type | Description |
|-----------|------|-------------|
| dateRange | `StatisticsDateRange` | Exact resolved window used to compute totals, charts, and category summaries |

**Notes**:

- Makes the summary result self-describing
- Prevents drill-down from having to recompute its own date boundaries

## Modified Entity: StatisticsState

**Module**: `presentation:statistics`  
**Purpose**: Existing UI state updated to retain the exact date range currently visible on Statistics.

| Attribute | Type | Description |
|-----------|------|-------------|
| period | `StatsPeriod` | Selected Statistics period |
| transactionType | `domain.statistics.model.TransactionType` | Selected expense or income mode |
| dateRange | `StatisticsDateRange?` | Exact resolved range currently backing the visible summary |

**Notes**:

- `dateRange` is required for exact-context drill-down navigation
- Existing totals, chart data, and category lists remain unchanged
- The parent Statistics screen may resolve a fresh live `dateRange` when it becomes visible again on return, while preserving the same selected period and transaction type

## New Entity: CategoryTransactionsRouteArgs

**Module**: `app` navigation layer  
**Purpose**: Type-safe route payload for category drill-down entered from Statistics.

| Attribute | Type | Description |
|-----------|------|-------------|
| categoryId | Long | Selected category identifier |
| categoryName | String | Display name for the drill-down header |
| categoryIcon | String | Icon token for the header when available |
| categoryColor | Long | Category color or accent token for the header |
| transactionTypeValue | String | Serialized active statistics type |
| periodName | String | Serialized active statistics period |
| startMillis | Long | Exact Statistics range start preserved at tap time |
| endMillis | Long | Exact Statistics range end preserved at tap time |

**Notes**:

- Uses primitives or strings so route serialization stays simple
- Carries enough context for the header and the exact filter even when the result list is empty
- Serves as a fallback snapshot when live category metadata cannot be resolved later

## New Entity: CategoryTransactionsState

**Module**: `presentation:statistics`  
**Purpose**: UI state for the dedicated category drill-down screen.

| Attribute | Type | Description |
|-----------|------|-------------|
| categoryId | Long | Filtered category id |
| categoryName | String | Header title |
| categoryIcon | String | Header icon token |
| categoryColor | Long | Header accent color |
| transactionTypeLabel | String | Human-readable expense or income label |
| periodLabel | String | Human-readable week, month, or year label |
| startMillis | Long | Preserved statistics range start |
| endMillis | Long | Preserved statistics range end |
| totalAmount | Double | Sum of currently filtered transactions |
| transactions | ImmutableList<Transaction> | Filtered transactions in newest-first order |
| isLoading | Boolean | Initial load state |
| isEmpty | Boolean | Derived empty-content state |

**Notes**:

- The drill-down session keeps `startMillis` and `endMillis` fixed even if the day changes while the user remains inside drill-down or transaction edit
- Header identity should prefer live category metadata resolved by `categoryId` and fall back to the route snapshot when the category cannot be resolved

## Modified Interface: TransactionRepository

**Module**: `domain:transactions`  
**Purpose**: Expose the filtered transaction observation required by Statistics drill-down.

**New operation**:

- `observeByCategoryTypeAndDateRange(categoryId: Long, transactionType: TransactionType, startMillis: Long, endMillis: Long): Flow<List<Transaction>>`

## Modified DAO: TransactionDao

**Module**: `core:database`  
**Purpose**: Provide a targeted Room query for drill-down filtering.

**New operation**:

- `observeByCategoryTypeAndDateRange(categoryId: Long, type: String, startDate: Long, endDate: Long): Flow<List<TransactionEntity>>`

**Query rules**:

- `isDeleted = 0`
- exact `categoryId`
- exact transaction `type`
- `date BETWEEN startDate AND endDate`
- ordered by `date DESC`

## Modified Component: SummaryStatCard

**Module**: `core:ui`  
**Purpose**: Allow the Statistics summary cards to act as the active type selector.

**New presentation properties**:

- `selected: Boolean`
- `onClick: (() -> Unit)?`

**Behavioral expectation**:

- selected card exposes active styling
- unselected card remains readable but subdued
- optional click support preserves reuse outside Statistics

## Relationships

```text
StatsPeriod
  └── StatisticsPeriodRangeResolver
        └── StatisticsDateRange(startMillis, endMillis)
              └── PeriodSummary(dateRange = ...)
                    └── StatisticsState(dateRange = ...)
                          └── CategoryTransactionsRouteArgs(startMillis, endMillis, ...)

StatisticsScreen(category row tap)
  └── CategoryTransactionsRouteArgs
        └── CategoryTransactionsViewModel
              └── TransactionRepository.observeByCategoryTypeAndDateRange(...)
```
