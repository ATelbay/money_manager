---
description: "Jetpack Compose rules in Money Manager: naming conventions (Screen/Route/ViewModel/State), State Hoisting, testTag, Material 3, UI scenarios for testing"
---

# Compose UI Guidelines

## Context

The project uses Jetpack Compose + Material 3 (BOM 2026.01.01). All UIs are built using the Screen (stateless) + Route (stateful) pattern. The project serves as the foundation for a master's thesis on UI automation, so testTag is a mandatory element.

**Key files:**
- `core/ui/src/.../theme/` — Color.kt, Type.kt, Theme.kt
- `core/ui/src/.../components/` — reusable components (MoneyManagerButton, MoneyManagerTextField, MoneyManagerCard)
- `presentation/*/src/.../ui/` — screens for each feature

## Process

### Creating a new screen
1. Create `{Feature}State.kt` — data class with all screen fields
2. Create `{Feature}ViewModel.kt` — @HiltViewModel, exposes `StateFlow<{Feature}State>`
3. Create `{Feature}Screen.kt` — stateless `@Composable`, accepts state + callback lambdas
4. Create `{Feature}Route.kt` — stateful wrapper, injects ViewModel, collects state via `collectAsStateWithLifecycle()`
5. Add `testTag` to all interactive elements

### Adding testTag
Format: `"screen:element"` — camelCase for both parts.

Examples:
```kotlin
Modifier.testTag("transactionList:fab")
Modifier.testTag("transactionEdit:amountField")
Modifier.testTag("import:selectPdf")
Modifier.testTag("settings:themeSelector")
```

## Naming Conventions

| Type | Pattern | Example |
|------|---------|---------|
| Screen | `{Feature}Screen.kt` | `TransactionListScreen.kt` |
| Route | `{Feature}Route.kt` | `TransactionListRoute.kt` |
| ViewModel | `{Feature}ViewModel.kt` | `TransactionListViewModel.kt` |
| State | `{Feature}State.kt` | `TransactionListState.kt` |
| UseCase | `{Action}{Entity}UseCase.kt` | `GetTransactionsUseCase.kt` |
| Repository | `{Entity}Repository.kt` (interface) | `TransactionRepository.kt` |
| Repository Impl | `{Entity}RepositoryImpl.kt` | `TransactionRepositoryImpl.kt` |

## Compose Rules

1. **Modifier** — always the first optional parameter:
   ```kotlin
   @Composable
   fun TransactionListScreen(
       state: TransactionListState,
       onTransactionClick: (Long) -> Unit,
       modifier: Modifier = Modifier,  // ← first optional
   )
   ```

2. **State Hoisting** — Screen does NOT know about ViewModel:
   ```kotlin
   // Route.kt (stateful)
   @Composable
   fun TransactionListRoute(viewModel: TransactionListViewModel = hiltViewModel()) {
       val state by viewModel.state.collectAsStateWithLifecycle()
       TransactionListScreen(state = state, onTransactionClick = { ... })
   }

   // Screen.kt (stateless) — accepts state + callbacks
   @Composable
   fun TransactionListScreen(state: TransactionListState, ...) { ... }
   ```

3. **ImmutableList** — for stable collections in State:
   ```kotlin
   data class TransactionListState(
       val transactions: ImmutableList<Transaction> = persistentListOf(),
   )
   ```

4. **testTag** — required for all interactive elements (buttons, input fields, lists, FAB, chips, etc.)

## UI Scenarios for Testing

| Scenario | Screen | Elements |
|----------|--------|----------|
| Multi-step flow | Onboarding | HorizontalPager, navigation buttons |
| Form validation | Transaction Edit, Create Account | TextField, validation, error states |
| List operations | Transaction List | LazyColumn, swipe-to-delete |
| Modal selection | Category picker | ModalBottomSheet with LazyVerticalGrid |
| Date picking | Transaction Edit | DatePickerDialog |
| Dropdown | Create Account | ExposedDropdownMenuBox for currency |
| Chart interaction | Statistics | Vico charts, touch feedback |
| CRUD | Categories | Create, Read, Update, Delete flow |
| Segmented control | Settings | SingleChoiceSegmentedButtonRow for theme |
| Navigation | All | Type-safe routes, back stack, bottom nav |
| AI import | Import | PDF picker, camera, Gemini parsing, editable preview |
| File picker | Import | ActivityResultContracts.OpenDocument for PDF |
| Inline editing | Import Preview | Editable fields (amount, date, type, category) |

## Quality Bar

- Every new interactive element MUST have a `testTag`
- Screen functions are always stateless — no `viewModel()` calls inside Screen
- Use `MoneyManagerTheme` from `:core:ui` for Preview
- Reuse components from `core/ui/components/` (MoneyManagerButton, MoneyManagerTextField, MoneyManagerCard)

## Anti-patterns

- DO NOT use `remember { mutableStateOf() }` for data that should live in ViewModel
- DO NOT call `hiltViewModel()` inside Screen — only in Route
- DO NOT forget `testTag` on interactive elements
- DO NOT use `List` in State data class — use `ImmutableList` (kotlinx.collections.immutable)
- DO NOT hardcode strings — use string resources
- DO NOT create Preview without `MoneyManagerTheme` wrapper
