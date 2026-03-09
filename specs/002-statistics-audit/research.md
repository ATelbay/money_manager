# Research: Statistics Screen Audit

## R1: Largest-Remainder Percentage Rounding

**Decision**: Use largest-remainder method (Hamilton's method) to distribute integer percentages that sum to exactly 100%.

**Rationale**: Standard approach used in election systems and financial software. Guarantees sum = 100%, is deterministic, and handles edge cases (single category = 100%, many small categories each get at least 1%).

**Algorithm**:
1. Compute raw percentage for each category: `rawPct = amount / total * 100`
2. Floor each: `flooredPct = floor(rawPct)`
3. Compute remainder: `remainder = rawPct - flooredPct`
4. Compute deficit: `deficit = 100 - sum(flooredPcts)`
5. Sort categories by remainder descending
6. Add 1 to the top `deficit` categories
7. Ensure minimum 1% for any non-zero category (steal from largest if needed)

**Alternatives considered**:
- `Math.round()` — can sum to 99% or 101%
- Bankers rounding — still can miss 100% target
- Last-category adjustment — can produce visually jarring results (e.g., one category jumps by 3%)

## R2: Bar Chart Aggregation Strategy by Period

**Decision**: UseCase returns different granularity per period:
- WEEK → 7 `DailyTotal` entries (one per day)
- MONTH → 30 `DailyTotal` entries (one per day)
- YEAR → 12 `MonthlyTotal` entries (one per calendar month)

**Rationale**: Keeps bar count fixed and readable. YEAR with 365 daily bars is unusable. Monthly aggregation (12 bars) is the natural mental model for yearly spending.

**Implementation**: Add `MonthlyTotal(year: Int, month: Int, amount: Double)` to domain models. UseCase groups transactions by `Calendar.YEAR + Calendar.MONTH` for YEAR period. Bar chart receives a unified `BarEntry(label: String, amount: Double)` list.

**Alternatives considered**:
- Weekly aggregation for YEAR (52 bars) — still too dense, labels would overlap
- Scrollable bar chart — adds complexity, inconsistent with WEEK/MONTH behavior

## R3: Expense/Income Toggle UI Pattern

**Decision**: Use Material 3 `SingleChoiceSegmentedButtonRow` with two segments: "Расходы" / "Доходы".

**Rationale**: M3-native component, two mutually exclusive options = perfect for segmented button. Placed below period selector, above charts. Consistent with existing chip-based period selector.

**Alternatives considered**:
- Tab row — visually heavier, not M3-idiomatic for 2 options
- Filter chip — implies multi-select, wrong semantics
- Dropdown — hidden state, poor discoverability

## R4: Donut Animation Stability Key

**Decision**: Use a derived key from category data content instead of the list reference.

**Rationale**: `ImmutableList` instances are recreated on every Flow emission even with identical data. Using `categories.map { it.categoryId to it.totalAmount }` as the `LaunchedEffect` key ensures animation only triggers when actual data changes.

**Alternatives considered**:
- `remember(key)` wrapper — still needs a stable key
- `distinctUntilChanged()` on Flow — would fix it at ViewModel level but doesn't address the root cause of using reference equality in Compose
- Both approaches combined: add `distinctUntilChanged()` in ViewModel AND use stable key in Compose for defense-in-depth

**Decision amended**: Use both:
1. `distinctUntilChanged()` after `.onEach` in ViewModel to prevent redundant state updates
2. Stable content-derived key in `LaunchedEffect` as safety net

## R5: Zero-Day Fill Strategy

**Decision**: Fill missing days/months with zero-amount entries in the UseCase, not in the UI.

**Rationale**: The UseCase knows the period boundaries and can generate a complete sequence. Putting this in the UI would leak domain logic into presentation. The chart composable should just render whatever it receives.

**Implementation**:
- For WEEK/MONTH: iterate from start date to end date by day, lookup grouped map, default to 0.0
- For YEAR: iterate 12 months backwards from current month, lookup grouped map, default to 0.0

## R6: Period Range Consistency (Rolling Windows)

**Decision**: Use day-count subtraction for all periods to ensure consistent rolling windows.

**Current behavior**: WEEK subtracts 6 days (correct, 7 days inclusive), MONTH subtracts 1 calendar month (inconsistent — could be 28-31 days), YEAR subtracts 1 year (could be 365 or 366 days).

**Fix**:
- WEEK: `today - 6 days` → 7 days (unchanged)
- MONTH: `today - 29 days` → 30 days (changed from `cal.add(MONTH, -1)`)
- YEAR: `today - 364 days` → 365 days (changed from `cal.add(YEAR, -1)`)

Start at 00:00:00.000, end at 23:59:59.999 of today.

## R7: Locale-Aware Date Labels

**Decision**: Use `DateFormat.getInstanceForSkeleton()` or `SimpleDateFormat` with locale-appropriate patterns.

**Implementation**:
- WEEK: `"EEE"` (Mon, Tue...) or `"dd.MM"` — use `"EEE"` for better readability with 7 bars
- MONTH: `"dd"` (day number only) — shown every 5th bar to prevent overlap
- YEAR: `"MMM"` (Jan, Feb...) — month abbreviation for 12 bars

**Rationale**: Short labels prevent overlap. Day-of-week names for WEEK are more intuitive than date numbers. Month names for YEAR are the natural label.

## R8: Error State UI Pattern

**Decision**: Full-screen error state with icon, message, and retry button, replacing the loading/content area.

**Rationale**: Consistent with Android/M3 error patterns. User can still switch periods (period selector remains visible above error state). Retry reloads data for current period.

**State model**: `StatisticsState.error: String? = null`. When non-null and `isLoading = false`, show error UI. On retry, set `isLoading = true, error = null` and re-call `loadSummary()`.
