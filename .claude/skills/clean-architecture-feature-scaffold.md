---
description: "Генератор новой фичи в Money Manager: создание feature-модуля с Clean Architecture (domain/data/ui/di), Repository, UseCase, ViewModel, Screen, Route, Hilt DI, навигация"
---

# Clean Architecture Feature Scaffold

## Context

Каждая фича в проекте — отдельный Gradle-модуль в `feature/` с паттерном Clean Architecture. Этот скилл описывает полный алгоритм создания новой фичи от нуля.

**Эталонный модуль для копирования паттернов:** `feature/transactions/` (наиболее полная фича с domain/data/ui/di).

## Process

### Шаг 1: Создать Gradle-модуль

1. Создать директорию `feature/{name}/`
2. Создать `feature/{name}/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.moneymanager.android.feature)
}

android {
    namespace = "com.atelbay.money_manager.feature.{name}"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    // другие core-зависимости по необходимости
}
```
3. Добавить в `settings.gradle.kts`:
```kotlin
include(":feature:{name}")
```
4. Добавить зависимость в `app/build.gradle.kts`:
```kotlin
implementation(project(":feature:{name}"))
```

### Шаг 2: Domain Layer

Создать пакет `feature/{name}/src/main/java/com/atelbay/money_manager/feature/{name}/domain/`

**Repository interface:**
```
domain/repository/{Entity}Repository.kt
```
```kotlin
interface {Entity}Repository {
    fun get{Entities}(): Flow<List<{Entity}>>
    fun get{Entity}ById(id: Long): Flow<{Entity}?>
    suspend fun save{Entity}(entity: {Entity})
    suspend fun delete{Entity}(id: Long)
}
```

**Use Cases** (по одному на операцию):
```
domain/usecase/Get{Entities}UseCase.kt
domain/usecase/Get{Entity}ByIdUseCase.kt
domain/usecase/Save{Entity}UseCase.kt
domain/usecase/Delete{Entity}UseCase.kt
```
```kotlin
class Get{Entities}UseCase @Inject constructor(
    private val repository: {Entity}Repository,
) {
    operator fun invoke(): Flow<List<{Entity}>> = repository.get{Entities}()
}
```

### Шаг 3: Data Layer

Создать пакет `data/`

**Mapper:**
```
data/mapper/{Entity}Mapper.kt
```
```kotlin
fun {Entity}Entity.toDomain(): {Entity} = ...
fun {Entity}.toEntity(): {Entity}Entity = ...
```

**Repository Implementation:**
```
data/repository/{Entity}RepositoryImpl.kt
```
```kotlin
class {Entity}RepositoryImpl @Inject constructor(
    private val dao: {Entity}Dao,
) : {Entity}Repository {
    override fun get{Entities}() = dao.getAll().map { list -> list.map { it.toDomain() } }
    // ...
}
```

### Шаг 4: DI Module

```
di/{Feature}Module.kt
```
```kotlin
@Module
@InstallIn(ViewModelComponent::class)
abstract class {Feature}Module {
    @Binds
    abstract fun bind{Entity}Repository(
        impl: {Entity}RepositoryImpl,
    ): {Entity}Repository
}
```

### Шаг 5: UI Layer

**State:**
```
ui/{subscreen}/{Feature}State.kt
```
```kotlin
data class {Feature}State(
    val items: ImmutableList<{Entity}> = persistentListOf(),
    val isLoading: Boolean = true,
    val error: String? = null,
)
```

**ViewModel:**
```
ui/{subscreen}/{Feature}ViewModel.kt
```
```kotlin
@HiltViewModel
class {Feature}ViewModel @Inject constructor(
    private val get{Entities}UseCase: Get{Entities}UseCase,
) : ViewModel() {
    private val _state = MutableStateFlow({Feature}State())
    val state: StateFlow<{Feature}State> = _state.asStateFlow()

    init { load{Entities}() }

    private fun load{Entities}() {
        get{Entities}UseCase()
            .onEach { items -> _state.update { it.copy(items = items.toImmutableList(), isLoading = false) } }
            .launchIn(viewModelScope)
    }
}
```

**Screen (stateless):**
```
ui/{subscreen}/{Feature}Screen.kt
```
```kotlin
@Composable
fun {Feature}Screen(
    state: {Feature}State,
    onItemClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    // UI с testTag на каждом интерактивном элементе
}
```

**Route (stateful):**
```
ui/{subscreen}/{Feature}Route.kt
```
```kotlin
@Composable
fun {Feature}Route(
    onNavigateBack: () -> Unit,
    viewModel: {Feature}ViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    {Feature}Screen(state = state, ...)
}
```

### Шаг 6: Навигация

1. Добавить destination в `app/.../navigation/Destinations.kt`:
```kotlin
@Serializable data object {Feature}  // или data class с параметрами
```

2. Добавить route в `MoneyManagerNavHost.kt`:
```kotlin
composable<{Feature}> {
    {Feature}Route(
        onNavigateBack = { navController.popBackStack() },
    )
}
```

3. Если top-level — добавить в `TopLevelDestination.kt`

### Шаг 7: testTag

Добавить testTag на все интерактивные элементы по конвенции `"featureName:element"`:
- FAB: `"{feature}List:fab"`
- List items: `"{feature}List:item_{id}"`
- Form fields: `"{feature}Edit:nameField"`, `"{feature}Edit:saveButton"`

## Чек-лист

- [ ] `build.gradle.kts` с `moneymanager.android.feature` plugin
- [ ] Модуль в `settings.gradle.kts`
- [ ] Зависимость в `app/build.gradle.kts`
- [ ] Repository interface в `domain/repository/`
- [ ] Use Cases в `domain/usecase/`
- [ ] Mapper в `data/mapper/`
- [ ] RepositoryImpl в `data/repository/`
- [ ] Hilt module в `di/`
- [ ] State data class с `ImmutableList`
- [ ] ViewModel с `StateFlow`
- [ ] Screen (stateless) с `testTag`
- [ ] Route (stateful) с `hiltViewModel()`
- [ ] Destination в `Destinations.kt`
- [ ] Route в `MoneyManagerNavHost.kt`

## Anti-patterns

- НЕ пропускай domain layer — даже для простых CRUD всегда делай Repository + UseCase
- НЕ делай ViewModel зависимым от Android-фреймворка (Context, Resources) — используй domain-модели
- НЕ забывай `ImmutableList` в State — обычный `List` вызывает лишние рекомпозиции
- НЕ создавай God-UseCase — один UseCase = одна операция
- НЕ забывай зарегистрировать модуль в `settings.gradle.kts` и `app/build.gradle.kts`
