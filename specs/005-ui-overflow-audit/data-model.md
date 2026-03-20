# Data Model: UI Overflow & Layout Audit

This feature is a **UI-only audit** — no new entities, database changes, or domain model modifications.

## Affected Display Models

No new data models are introduced. The audit modifies only how existing data is **rendered** in composables.

### Existing Models Displayed (no changes)

| Model | Key Fields for Display | Where Displayed |
|-------|----------------------|-----------------|
| Account | `name: String`, `balance: Double`, `currency: String` | BalanceCard, AccountCard, AccountListScreen |
| Transaction | `amount: Double`, `description: String`, `categoryName: String`, `date: Long` | TransactionListItem, TransactionEditScreen |
| Category | `name: String`, `icon: String`, `color: Int` | CategoryListScreen, CategoryPicker, StatisticsScreen |
| PeriodSummary | `income: Double`, `expense: Double` | IncomeExpenseCard |
| CategorySummary | `categoryName: String`, `amount: Double`, `percentage: Float` | StatisticsScreen |
| ParsedTransaction | `amount: Double`, `description: String`, `date: String` | ParsedTransactionItem |
| User (Firebase) | `displayName: String`, `email: String`, `photoUrl: String` | SignInScreen, SettingsScreen |

## State Changes

No ViewModel state changes. All modifications are at the Compose UI layer — adding `autoSize` parameters, `maxLines`, `TextOverflow.Ellipsis`, and layout constraints to existing `Text` composables.
