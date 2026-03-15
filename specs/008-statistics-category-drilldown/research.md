# Research: Statistics UX Cleanup + Category Transaction Drill-Down

**Branch**: `008-statistics-category-drilldown` | **Date**: 2026-03-15

## R-001: Remove the duplicate top toggle and reuse the summary cards

**Decision**: Remove the standalone expense/income toggle row from Statistics and make the existing expense and income summary cards the only transaction-type selector.

**Rationale**: The screen already surfaces both totals. Reusing those cards removes duplicate controls while preserving comparison context.

**Alternatives considered**:

- Keep the separate toggle and make cards passive: rejected because it preserves the ambiguity the user wants removed.
- Remove the cards and keep only a segmented control: rejected because it discards useful summary context and does not match the requested interaction model.

## R-002: Keep both summary cards visible with explicit active and inactive styling

**Decision**: Both summary cards remain visible at all times. The active card gets clear selected styling, and the inactive card stays readable but subdued.

**Rationale**: Users still need to compare expense and income totals at a glance, but the currently selected dataset must be obvious.

**Alternatives considered**:

- Hide the inactive card: rejected because it removes comparative context.
- Style both cards equally: rejected because it weakens the selector affordance.

## R-003: Category rows are the only drill-down trigger

**Decision**: Only category rows in the breakdown section open drill-down. The donut chart, slices, and legend remain passive.

**Rationale**: A single explicit entry point is easier to learn, avoids accidental navigation, and respects the requested scope boundary of "no second drill-down entry point."

**Alternatives considered**:

- Make donut slices tappable too: rejected because it adds a second entry point and changes chart behavior beyond the requested refinement.
- Make the entire category card tappable without row-level affordance: rejected because the user explicitly scoped drill-down to category rows.

## R-004: Use a dedicated detail screen inside Statistics, not inline expansion or Home reuse

**Decision**: Tapping a category row opens a dedicated detail screen within the Statistics flow.

**Rationale**: Statistics is already a dense overview screen. A dedicated detail screen keeps the overview focused, avoids deep inline content, and prevents top-level navigation jumps into Home.

**Alternatives considered**:

- Inline expansion under category rows: rejected because it overloads the Statistics screen and scales poorly for long transaction lists.
- Reuse the Home transaction list route: rejected because it would change top-level destination context and inherit Home-specific behavior that Statistics should not depend on.

## R-005: Preserve the exact resolved date range from Statistics at navigation time

**Decision**: Keep a shared Statistics period-range resolver for summary generation, but also preserve the resolved `startMillis` and `endMillis` in Statistics state and pass them through drill-down navigation arguments.

**Rationale**: The feature requires the drill-down screen to reflect the exact same date range the user saw on Statistics. Passing only the period and recomputing later can drift at boundaries, especially around day rollover or future changes to Statistics period semantics.

**Alternatives considered**:

- Recompute the date range inside drill-down from `StatsPeriod` alone: rejected because it does not guarantee the exact same resolved range.
- Pass only raw timestamps without a shared resolver: rejected because summary generation and navigation still need one source of truth for the original range calculation.

## R-006: Filter transactions in the data layer by category, type, and date range

**Decision**: Add a targeted DAO and repository path that filters by category, transaction type, and date range before mapping to domain models.

**Rationale**: The repository currently exposes `observeAll()`, while the DAO already contains category-oriented queries. Filtering in the data layer keeps drill-down efficient and reactive as the dataset grows.

**Alternatives considered**:

- Filter `observeAll()` in the drill-down ViewModel: rejected because it scales poorly and broadens reactive churn.
- Filter by category only in the DAO and apply type/date logic in UI: rejected because it still leaves key filter logic outside the data layer.

## R-007: Keep enum unification out of scope and map at the boundary

**Decision**: Keep the existing `domain.statistics.model.TransactionType` and `core.model.TransactionType` types as-is and add a thin mapper at the drill-down boundary.

**Rationale**: The mismatch is broader than this feature. A mapper solves the immediate need without expanding scope.

**Alternatives considered**:

- Replace the statistics type with the core type everywhere: rejected because it widens the blast radius of a UX refinement task.
- Use raw strings throughout the UI: rejected because it weakens type safety unnecessarily.

## R-008: Pass category presentation metadata through the route

**Decision**: The drill-down route carries `categoryId`, `categoryName`, `categoryIcon`, `categoryColor`, `transactionType`, `period`, `startMillis`, and `endMillis`.

**Rationale**: The header must render immediately and stay stable even when the filtered transaction list is empty. Primitive route arguments also keep navigation serialization simple.

**Alternatives considered**:

- Load category identity only from matching transactions: rejected because the header would collapse when there are no results.
- Pass entire model objects through the route: rejected because the existing navigation pattern favors serializable primitives.

## R-009: Rely on back-stack preservation plus reactive flows after edits

**Decision**: Keep Statistics on the back stack so the selected period and type remain unchanged on return, and rely on reactive flows so edits and deletions refresh both screens.

**Rationale**: The requested behavior is state preservation, not a custom refresh mechanism. Back-stack continuity plus reactive observation is the lowest-risk path.

**Alternatives considered**:

- Reset Statistics on return and restore state manually: rejected because it adds unnecessary restoration logic and more failure modes.
- Add an explicit "refresh on back" event: rejected because the underlying flows should already provide the required consistency.

## R-010: Keep drill-down pinned to the captured range even across day rollover

**Decision**: Once drill-down opens, it remains pinned to the captured `startMillis` and `endMillis` for the lifetime of that drill-down session, even if the current day changes while the user stays inside drill-down or the downstream transaction edit flow.

**Rationale**: The user asked for drill-down to preserve the exact Statistics context they tapped from. Recomputing the range mid-session would silently change the meaning of the list.

**Alternatives considered**:

- Recompute the range whenever the clock crosses into a new day: rejected because drill-down would no longer match the tapped Statistics context.
- Freeze Statistics itself after return until manual refresh: rejected because the requirement is to preserve period/type selection, not to suppress normal live range behavior on the parent screen.

## R-011: Prefer live category metadata, fall back to the tapped snapshot

**Decision**: Drill-down should resolve the selected category by id and use the latest category metadata when available, while retaining the route snapshot as a fallback for empty or unresolved cases.

**Rationale**: The tapped snapshot guarantees a stable header even if the category is missing, but users should still see updated category naming or color changes when the data remains available.

**Alternatives considered**:

- Always use the tapped snapshot: rejected because the header would become stale after category edits.
- Depend entirely on live category lookup with no snapshot fallback: rejected because the header would degrade when the category is deleted or cannot be resolved.
