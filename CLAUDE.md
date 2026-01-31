# Spendee — Personal Finance Tracker

## Обзор проекта

Spendee — Android-приложение для учёта личных финансов. Разрабатывается как часть магистерской диссертации, посвящённой UI-автоматизации тестирования.

**Цель:** создать приложение с разнообразными UI-паттернами для последующего покрытия автоматизированными тестами.

## Технический стек

| Компонент | Технология |
|-----------|------------|
| UI | Jetpack Compose + Material 3 |
| DI | Hilt |
| Database | Room |
| Navigation | Navigation Compose 2.8+ (type-safe) |
| Architecture | MVVM + Clean Architecture (единый UI State) |
| Async | Coroutines + Flow |
| Charts | Vico |
| Build | Version Catalogs + Convention Plugins |
| CI/CD | GitHub Actions → Firebase App Distribution → Play Store |

## Архитектура

### Модульная структура

```
app/
├── src/main/java/com/example/spendee/
│   ├── MainActivity.kt
│   ├── SpendeeApp.kt
│   └── navigation/
│       └── SpendeeNavHost.kt
│
├── core/
│   ├── database/          # Room DB, entities, DAOs
│   ├── di/                # Hilt modules
│   ├── ui/                # Общие Compose-компоненты
│   │   ├── components/    # Button, Card, TextField и т.д.
│   │   └── theme/         # Colors, Typography, Theme
│   └── util/              # Extensions, helpers
│
├── feature/
│   ├── onboarding/
│   │   ├── data/
│   │   ├── domain/
│   │   └── ui/
│   │
│   ├── transactions/
│   │   ├── data/
│   │   │   ├── repository/
│   │   │   └── mapper/
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   ├── repository/
│   │   │   └── usecase/
│   │   └── ui/
│   │       ├── list/
│   │       │   ├── TransactionListScreen.kt
│   │       │   └── TransactionListViewModel.kt
│   │       └── edit/
│   │           ├── TransactionEditScreen.kt
│   │           └── TransactionEditViewModel.kt
│   │
│   ├── categories/
│   │   ├── data/
│   │   ├── domain/
│   │   └── ui/
│   │
│   ├── statistics/
│   │   ├── data/
│   │   ├── domain/
│   │   └── ui/
│   │
│   └── accounts/
│       ├── data/
│       ├── domain/
│       └── ui/
```

### Паттерн UI State

```kotlin
// Единый state для экрана
data class TransactionListState(
    val transactions: List<TransactionUi> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedPeriod: Period = Period.MONTH
)

// ViewModel с прямыми методами (не Intent)
@HiltViewModel
class TransactionListViewModel @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase
) : ViewModel() {
    
    private val _state = MutableStateFlow(TransactionListState())
    val state: StateFlow<TransactionListState> = _state.asStateFlow()
    
    fun loadTransactions() { /* ... */ }
    fun selectPeriod(period: Period) { /* ... */ }
    fun deleteTransaction(id: Long) { /* ... */ }
}

// Screen получает state и callbacks
@Composable
fun TransactionListScreen(
    state: TransactionListState,
    onTransactionClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onPeriodChange: (Period) -> Unit,
    modifier: Modifier = Modifier
)
```

### Навигация (Type-Safe)

```kotlin
// Destinations
@Serializable data object Home
@Serializable data object Onboarding
@Serializable data class TransactionEdit(val id: Long? = null)
@Serializable data class CategoryEdit(val id: Long? = null)
@Serializable data object Statistics
@Serializable data object Accounts

// NavHost
@Composable
fun SpendeeNavHost(navController: NavHostController) {
    NavHost(navController, startDestination = Home) {
        composable<Home> { 
            HomeRoute(
                onTransactionClick = { navController.navigate(TransactionEdit(it)) },
                onAddClick = { navController.navigate(TransactionEdit()) }
            )
        }
        composable<TransactionEdit> { backStackEntry ->
            val args: TransactionEdit = backStackEntry.toRoute()
            TransactionEditRoute(
                transactionId = args.id,
                onBack = { navController.popBackStack() }
            )
        }
        // ...
    }
}
```

## База данных (Room)

### Entities

```kotlin
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val currency: String,
    val balance: Double,
    val createdAt: Long
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,        // имя иконки из Material Icons
    val color: Long,         // ARGB цвет
    val type: String,        // "income" | "expense"
    val isDefault: Boolean   // предустановленная категория
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"]),
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["categoryId"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: String,        // "income" | "expense"
    val categoryId: Long,
    val accountId: Long,
    val note: String?,
    val date: Long,          // timestamp
    val createdAt: Long
)
```

## Приоритеты реализации

### P0 — Критический путь
1. **Core**: DI setup, Room database, Theme
2. **Onboarding**: Welcome screens, Create account, Currency selection
3. **Transactions**: List (главный экран), Add/Edit form

### P1 — Основной функционал
4. **Categories**: List, Add/Edit with icon & color picker
5. **Statistics**: Pie chart по категориям, Line chart динамики

### P2 — Расширение
6. **Accounts**: Manage multiple accounts, Switch between them

## UI-сценарии для тестирования

Приложение специально проектируется для покрытия разнообразных UI-паттернов:

| Сценарий | Экран | Элементы |
|----------|-------|----------|
| Multi-step flow | Onboarding | HorizontalPager, кнопки навигации |
| Form validation | Transaction Edit | TextField, валидация, error states |
| List operations | Transaction List | LazyColumn, swipe-to-delete, pull-refresh |
| Modal selection | Category picker | BottomSheet / Dialog с списком |
| Date picking | Transaction Edit | DatePickerDialog |
| Chart interaction | Statistics | Vico charts, touch feedback |
| CRUD | Categories | Create, Read, Update, Delete flow |
| Navigation | Все | Deep links, back stack |

## Code Style

### Naming
- Screens: `{Feature}Screen.kt` — stateless composable
- Routes: `{Feature}Route.kt` — stateful wrapper с ViewModel
- ViewModels: `{Feature}ViewModel.kt`
- UseCases: `{Action}{Entity}UseCase.kt` (e.g., `GetTransactionsUseCase`)
- Repositories: `{Entity}Repository.kt` (interface), `{Entity}RepositoryImpl.kt`

### Compose
- Preview для каждого значимого компонента
- Modifier как первый необязательный параметр
- State hoisting: Screen не знает о ViewModel
- Использовать `ImmutableList` для стабильных коллекций

### Тестирование
- Каждый ViewModel покрыт unit-тестами
- UI-тесты с использованием `testTag` для ключевых элементов
- `testTag` naming: `"screen:element"` (e.g., `"transactionList:fab"`, `"transactionEdit:amountField"`)

## Полезные команды

```bash
# Сборка
./gradlew assembleDebug

# Тесты
./gradlew test                    # unit-тесты
./gradlew connectedAndroidTest    # UI-тесты

# Lint & Analysis
./gradlew lint
./gradlew detekt

# Dependency updates
./gradlew dependencyUpdates
```

## TODO / Не в MVP

- [ ] Бюджеты и лимиты по категориям
- [ ] Повторяющиеся транзакции
- [ ] Экспорт в CSV/PDF
- [ ] PIN-код / биометрия
- [ ] Мультивалютность с конвертацией
- [ ] Облачная синхронизация
- [ ] Widgets
