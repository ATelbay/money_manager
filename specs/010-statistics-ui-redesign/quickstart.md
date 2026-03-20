# Quickstart: Statistics Screen UI Redesign

**Branch**: `010-statistics-ui-redesign`

## Key Files

| File | Purpose | Change Type |
|------|---------|-------------|
| `core/ui/.../theme/Color.kt` | Design token definitions | Update values |
| `core/ui/.../components/MoneyManagerSegmentedButton.kt` | Segmented control | Refactor styling |
| `presentation/statistics/.../ui/StatisticsScreen.kt` | Main screen (~960 lines) | Major refactor |
| `presentation/statistics/.../ui/StatisticsViewModel.kt` | Screen ViewModel | Add selectedMonth state |
| `domain/statistics/.../usecase/StatisticsPeriodRangeResolver.kt` | Date range computation | Add optional anchor param |
| `domain/statistics/.../usecase/GetPeriodSummaryUseCase.kt` | Data loading use case | Add optional anchor param |

## Build & Verify

```bash
# Build
./gradlew assembleDebug

# Run tests (statistics module)
./gradlew :presentation:statistics:test
./gradlew :domain:statistics:test

# Full lint + test
./gradlew test lint detekt
```

## Architecture Constraints

- `presentation:statistics` depends on `domain:statistics` and `core:ui`
- `presentation:statistics` MUST NOT depend on `core:database`
- All new composables need `testTag` modifiers
- Colors MUST come from `MoneyManagerTheme.colors` — no hardcoded hex in composables

## Implementation Order

1. **Color tokens** → `core:ui` (affects all screens — verify visually)
2. **Segmented button refactor** → `core:ui` (update styling, add weight support)
3. **Domain anchor param** → `domain:statistics` (backward-compatible addition)
4. **ViewModel selectedMonth** → `presentation:statistics` (new state field + setter)
5. **Chart card restructure** → `presentation:statistics` (biggest change — merge chart + toggle + total)
6. **Period selector** → `presentation:statistics` (swap chips for segmented button)
7. **Donut + legend** → `presentation:statistics` (horizontal layout + expand/collapse)
8. **Calendar pill + month picker** → `presentation:statistics` (header + dialog)
9. **Scroll indicator** → `presentation:statistics` (below bar chart)
10. **Test tag preservation** → verify all existing tags still present
