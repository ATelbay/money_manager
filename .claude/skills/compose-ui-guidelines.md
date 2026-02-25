---
description: "Правила Jetpack Compose в Money Manager: naming conventions (Screen/Route/ViewModel/State), State Hoisting, testTag, Material 3, UI-сценарии для тестирования"
---

# Compose UI Guidelines

## Context

Проект использует Jetpack Compose + Material 3 (BOM 2026.01.01). Все UI построены по паттерну Screen (stateless) + Route (stateful). Проект является базой для магистерской по UI-автоматизации, поэтому testTag — обязательный элемент.

**Ключевые файлы:**
- `core/ui/src/.../theme/` — Color.kt, Type.kt, Theme.kt
- `core/ui/src/.../components/` — переиспользуемые компоненты (MoneyManagerButton, MoneyManagerTextField, MoneyManagerCard)
- `feature/*/src/.../ui/` — экраны каждой фичи

## Process

### Создание нового экрана
1. Создать `{Feature}State.kt` — data class со всеми полями экрана
2. Создать `{Feature}ViewModel.kt` — @HiltViewModel, выставляет `StateFlow<{Feature}State>`
3. Создать `{Feature}Screen.kt` — stateless `@Composable`, принимает state + callback-лямбды
4. Создать `{Feature}Route.kt` — stateful wrapper, инжектит ViewModel, собирает state через `collectAsStateWithLifecycle()`
5. Добавить `testTag` на все интерактивные элементы

### Добавление testTag
Формат: `"screen:element"` — camelCase для обеих частей.

Примеры:
```kotlin
Modifier.testTag("transactionList:fab")
Modifier.testTag("transactionEdit:amountField")
Modifier.testTag("import:selectPdf")
Modifier.testTag("settings:themeSelector")
```

## Naming Conventions

| Тип | Паттерн | Пример |
|-----|---------|--------|
| Screen | `{Feature}Screen.kt` | `TransactionListScreen.kt` |
| Route | `{Feature}Route.kt` | `TransactionListRoute.kt` |
| ViewModel | `{Feature}ViewModel.kt` | `TransactionListViewModel.kt` |
| State | `{Feature}State.kt` | `TransactionListState.kt` |
| UseCase | `{Action}{Entity}UseCase.kt` | `GetTransactionsUseCase.kt` |
| Repository | `{Entity}Repository.kt` (interface) | `TransactionRepository.kt` |
| Repository Impl | `{Entity}RepositoryImpl.kt` | `TransactionRepositoryImpl.kt` |

## Compose Rules

1. **Modifier** — всегда первый необязательный параметр:
   ```kotlin
   @Composable
   fun TransactionListScreen(
       state: TransactionListState,
       onTransactionClick: (Long) -> Unit,
       modifier: Modifier = Modifier,  // ← первый optional
   )
   ```

2. **State Hoisting** — Screen НЕ знает о ViewModel:
   ```kotlin
   // Route.kt (stateful)
   @Composable
   fun TransactionListRoute(viewModel: TransactionListViewModel = hiltViewModel()) {
       val state by viewModel.state.collectAsStateWithLifecycle()
       TransactionListScreen(state = state, onTransactionClick = { ... })
   }

   // Screen.kt (stateless) — принимает state + callbacks
   @Composable
   fun TransactionListScreen(state: TransactionListState, ...) { ... }
   ```

3. **ImmutableList** — для стабильных коллекций в State:
   ```kotlin
   data class TransactionListState(
       val transactions: ImmutableList<Transaction> = persistentListOf(),
   )
   ```

4. **testTag** — обязателен для всех интерактивных элементов (кнопки, поля ввода, списки, FAB, chips и т.д.)

## UI-сценарии для тестирования

| Сценарий | Экран | Элементы |
|----------|-------|----------|
| Multi-step flow | Onboarding | HorizontalPager, кнопки навигации |
| Form validation | Transaction Edit, Create Account | TextField, валидация, error states |
| List operations | Transaction List | LazyColumn, swipe-to-delete |
| Modal selection | Category picker | ModalBottomSheet с LazyVerticalGrid |
| Date picking | Transaction Edit | DatePickerDialog |
| Dropdown | Create Account | ExposedDropdownMenuBox для валюты |
| Chart interaction | Statistics | Vico charts, touch feedback |
| CRUD | Categories | Create, Read, Update, Delete flow |
| Segmented control | Settings | SingleChoiceSegmentedButtonRow для темы |
| Navigation | Все | Type-safe routes, back stack, bottom nav |
| AI import | Import | PDF picker, camera, Gemini parsing, editable preview |
| File picker | Import | ActivityResultContracts.OpenDocument для PDF |
| Inline editing | Import Preview | Editable fields (amount, date, type, category) |

## Quality Bar

- Каждый новый интерактивный элемент ДОЛЖЕН иметь `testTag`
- Screen-функции всегда stateless — никаких `viewModel()` вызовов внутри Screen
- Используй `MoneyManagerTheme` из `:core:ui` для Preview
- Переиспользуй компоненты из `core/ui/components/` (MoneyManagerButton, MoneyManagerTextField, MoneyManagerCard)

## Anti-patterns

- НЕ используй `remember { mutableStateOf() }` для данных, которые должны жить во ViewModel
- НЕ вызывай `hiltViewModel()` внутри Screen — только в Route
- НЕ забывай `testTag` на интерактивных элементах
- НЕ используй `List` в State data class — используй `ImmutableList` (kotlinx.collections.immutable)
- НЕ хардкодь строки — используй string resources
- НЕ создавай Preview без `MoneyManagerTheme` обёртки
