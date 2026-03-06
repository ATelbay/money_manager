---
description: "Генератор новой фичи в Money Manager: создание 3 Gradle-модулей (domain/data/presentation) с Clean Architecture — Repository, UseCase, ViewModel, Screen, Route, Hilt DI, навигация"
---

# Clean Architecture Feature Scaffold

## Context

Каждая фича в проекте — **3 отдельных Gradle-модуля** в layer-centric структуре:
- `domain/{name}/` — repository interface + use cases
- `data/{name}/` — repository impl + mapper + DI
- `presentation/{name}/` — State, ViewModel, Screen, Route

**Директории `feature/` не существует** — не используй её как образец.

**Эталонные модули для копирования паттернов:**
- `domain/transactions/` — TransactionRepository + UseCases
- `data/transactions/` — TransactionRepositoryImpl + mapper + DI
- `presentation/transactions/` — полный UI стек (State, ViewModel, Screen, Route)

## Process

### Шаг 1: Создать `domain/{name}/`

Создать `domain/{name}/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.moneymanager.android.library)
    // alias(libs.plugins.moneymanager.android.hilt)  // добавь только если UseCase требует @Inject deps
}

android {
    namespace = "com.atelbay.money_manager.domain.{name}"
}

dependencies {
    implementation(project(":core:model"))
}
```

**Repository interface** (`domain/{name}/src/main/java/com/atelbay/money_manager/domain/{name}/repository/{Entity}Repository.kt`):
```kotlin
interface {Entity}Repository {
    fun get{Entities}(): Flow<List<{Entity}>>
    fun get{Entity}ById(id: Long): Flow<{Entity}?>
    suspend fun save{Entity}(entity: {Entity})
    suspend fun delete{Entity}(id: Long)
}
```

**Use Cases** (один UseCase = одна операция, в `domain/{name}/.../usecase/`):
```kotlin
class Get{Entities}UseCase @Inject constructor(
    private val repository: {Entity}Repository,
) {
    operator fun invoke(): Flow<List<{Entity}>> = repository.get{Entities}()
}

class Save{Entity}UseCase @Inject constructor(
    private val repository: {Entity}Repository,
) {
    suspend operator fun invoke(entity: {Entity}) = repository.save{Entity}(entity)
}
```

### Шаг 2: Создать `data/{name}/`

Создать `data/{name}/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.moneymanager.android.library)
    alias(libs.plugins.moneymanager.android.hilt)
}

android {
    namespace = "com.atelbay.money_manager.data.{name}"
}

dependencies {
    implementation(project(":domain:{name}"))
    implementation(project(":core:database"))
    implementation(project(":core:model"))
}
```

**Mapper** (`data/{name}/.../mapper/{Entity}Mapper.kt`):
```kotlin
fun {Entity}Entity.toDomain(): {Entity} = {Entity}(
    id = id,
    // map fields...
)

fun {Entity}.toEntity(): {Entity}Entity = {Entity}Entity(
    id = id ?: 0,
    // map fields...
)
```

**Repository Implementation** (`data/{name}/.../repository/{Entity}RepositoryImpl.kt`):
```kotlin
class {Entity}RepositoryImpl @Inject constructor(
    private val dao: {Entity}Dao,
) : {Entity}Repository {
    override fun get{Entities}() = dao.getAll().map { list -> list.map { it.toDomain() } }
    override fun get{Entity}ById(id: Long) = dao.getById(id).map { it?.toDomain() }
    override suspend fun save{Entity}(entity: {Entity}) = dao.insert(entity.toEntity())
    override suspend fun delete{Entity}(id: Long) = dao.deleteById(id)
}
```

**Hilt DI Module** (`data/{name}/.../di/{Feature}Module.kt`):
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class {Feature}Module {
    @Binds
    abstract fun bind{Entity}Repository(
        impl: {Entity}RepositoryImpl,
    ): {Entity}Repository
}
```

### Шаг 3: Создать `presentation/{name}/`

Создать `presentation/{name}/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.moneymanager.android.feature)
}

android {
    namespace = "com.atelbay.money_manager.presentation.{name}"
}

dependencies {
    implementation(project(":domain:{name}"))
    implementation(project(":core:model"))
    // НЕ добавляй core:database — presentation не зависит от DB напрямую
}
```

**State** (`presentation/{name}/.../ui/{subscreen}/{Feature}State.kt`):
```kotlin
data class {Feature}State(
    val items: ImmutableList<{Entity}> = persistentListOf(),
    val isLoading: Boolean = true,
    val error: String? = null,
)
```

**ViewModel** (`presentation/{name}/.../ui/{subscreen}/{Feature}ViewModel.kt`):
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
            .onEach { items ->
                _state.update { it.copy(items = items.toImmutableList(), isLoading = false) }
            }
            .launchIn(viewModelScope)
    }
}
```

**Screen (stateless)** (`presentation/{name}/.../ui/{subscreen}/{Feature}Screen.kt`):
```kotlin
@Composable
fun {Feature}Screen(
    state: {Feature}State,
    onItemClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.testTag("{feature}List:list")) {
        items(state.items, key = { it.id }) { item ->
            Item(
                item = item,
                onClick = { onItemClick(item.id) },
                modifier = Modifier.testTag("{feature}List:item_${item.id}"),
            )
        }
    }
}
```

**Route (stateful)** (`presentation/{name}/.../ui/{subscreen}/{Feature}Route.kt`):
```kotlin
@Composable
fun {Feature}Route(
    onNavigateBack: () -> Unit,
    viewModel: {Feature}ViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    {Feature}Screen(
        state = state,
        onItemClick = { /* navigate */ },
    )
}
```

### Шаг 4: Зарегистрировать в `settings.gradle.kts`

```kotlin
include(":domain:{name}")
include(":data:{name}")
include(":presentation:{name}")
```

### Шаг 5: Добавить в `app/build.gradle.kts`

Необходимо добавить **оба** модуля — presentation для UI и data для Hilt DI wiring:

```kotlin
// Presentation (UI + ViewModel + навигация)
implementation(project(":presentation:{name}"))
// Data (обязательно для Hilt — data-модуль должен быть на classpath :app)
implementation(project(":data:{name}"))
```

> Без `data:{name}` в `app/build.gradle.kts` Hilt не найдёт биндинги репозитория и упадёт при сборке.

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

Конвенция: `"featureName:element"` (camelCase):
- FAB: `"{feature}List:fab"`
- List items: `"{feature}List:item_{id}"`
- Form fields: `"{feature}Edit:nameField"`, `"{feature}Edit:saveButton"`

## Чек-лист

- [ ] `domain/{name}/build.gradle.kts` с `moneymanager.android.library`
- [ ] `data/{name}/build.gradle.kts` с library + hilt
- [ ] `presentation/{name}/build.gradle.kts` с `moneymanager.android.feature`
- [ ] Все 3 модуля в `settings.gradle.kts`
- [ ] Только `:presentation:{name}` в `app/build.gradle.kts`
- [ ] Repository interface в `domain/{name}/.../repository/`
- [ ] Use Cases в `domain/{name}/.../usecase/`
- [ ] Mapper в `data/{name}/.../mapper/`
- [ ] RepositoryImpl в `data/{name}/.../repository/`
- [ ] Hilt DI module в `data/{name}/.../di/`
- [ ] State data class с `ImmutableList`
- [ ] ViewModel с `StateFlow`
- [ ] Screen (stateless) с `testTag` на всех интерактивных элементах
- [ ] Route (stateful) с `hiltViewModel()`
- [ ] Destination в `Destinations.kt`
- [ ] Route в `MoneyManagerNavHost.kt`

## Anti-patterns

- НЕ создавай единый `feature/{name}/` модуль — всегда 3 слоя: domain + data + presentation
- НЕ добавляй `core:database` в presentation-модуль
- НЕ пропускай domain layer — даже для простых CRUD всегда Repository + UseCase
- НЕ делай God-UseCase — один UseCase = одна операция
- НЕ используй `ViewModelComponent` для репозиториев — используй `SingletonComponent`
- НЕ создавай `@Module` без `@InstallIn`
- НЕ используй `kapt` — проект на KSP
- НЕ хардкодь версии в build.gradle.kts — используй `libs.versions.toml`
- НЕ забывай регистрировать все 3 модуля в `settings.gradle.kts`
