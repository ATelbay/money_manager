---
description: "New feature generator for Money Manager: creates 3 Gradle modules (domain/data/presentation) with Clean Architecture — Repository, UseCase, ViewModel, Screen, Route, Hilt DI, navigation"
---

# Clean Architecture Feature Scaffold

## Context

Each feature in the project is **3 separate Gradle modules** in a layer-centric structure:
- `domain/{name}/` — repository interface + use cases
- `data/{name}/` — repository impl + mapper + DI
- `presentation/{name}/` — State, ViewModel, Screen, Route

**The `feature/` directory does not exist** — do not use it as a reference.

**Reference modules for copying patterns:**
- `domain/transactions/` — TransactionRepository + UseCases
- `data/transactions/` — TransactionRepositoryImpl + mapper + DI
- `presentation/transactions/` — full UI stack (State, ViewModel, Screen, Route)

## Process

### Step 1: Create `domain/{name}/`

Create `domain/{name}/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.moneymanager.android.library)
    // alias(libs.plugins.moneymanager.android.hilt)  // add only if UseCase requires @Inject deps
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

**Use Cases** (one UseCase = one operation, in `domain/{name}/.../usecase/`):
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

### Step 2: Create `data/{name}/`

Create `data/{name}/build.gradle.kts`:
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

### Step 3: Create `presentation/{name}/`

Create `presentation/{name}/build.gradle.kts`:
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
    // Do NOT add core:database — presentation must not depend on DB directly
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

### Step 4: Register in `settings.gradle.kts`

```kotlin
include(":domain:{name}")
include(":data:{name}")
include(":presentation:{name}")
```

### Step 5: Add to `app/build.gradle.kts`

Both modules must be added — presentation for UI and data for Hilt DI wiring:

```kotlin
// Presentation (UI + ViewModel + navigation)
implementation(project(":presentation:{name}"))
// Data (required for Hilt — data module must be on the :app classpath)
implementation(project(":data:{name}"))
```

> Without `data:{name}` in `app/build.gradle.kts` Hilt cannot find repository bindings and will fail at build time.

### Step 6: Navigation

1. Add destination in `app/.../navigation/Destinations.kt`:
```kotlin
@Serializable data object {Feature}  // or data class with parameters
```

2. Add route in `MoneyManagerNavHost.kt`:
```kotlin
composable<{Feature}> {
    {Feature}Route(
        onNavigateBack = { navController.popBackStack() },
    )
}
```

3. If top-level — add to `TopLevelDestination.kt`

### Step 7: testTag

Convention: `"featureName:element"` (camelCase):
- FAB: `"{feature}List:fab"`
- List items: `"{feature}List:item_{id}"`
- Form fields: `"{feature}Edit:nameField"`, `"{feature}Edit:saveButton"`

## Checklist

- [ ] `domain/{name}/build.gradle.kts` with `moneymanager.android.library`
- [ ] `data/{name}/build.gradle.kts` with library + hilt
- [ ] `presentation/{name}/build.gradle.kts` with `moneymanager.android.feature`
- [ ] All 3 modules in `settings.gradle.kts`
- [ ] Both `:presentation:{name}` AND `:data:{name}` in `app/build.gradle.kts`
- [ ] Repository interface in `domain/{name}/.../repository/`
- [ ] Use Cases in `domain/{name}/.../usecase/`
- [ ] Mapper in `data/{name}/.../mapper/`
- [ ] RepositoryImpl in `data/{name}/.../repository/`
- [ ] Hilt DI module in `data/{name}/.../di/`
- [ ] State data class with `ImmutableList`
- [ ] ViewModel with `StateFlow`
- [ ] Screen (stateless) with `testTag` on all interactive elements
- [ ] Route (stateful) with `hiltViewModel()`
- [ ] Destination in `Destinations.kt`
- [ ] Route in `MoneyManagerNavHost.kt`

## Anti-patterns

- Do NOT create a single `feature/{name}/` module — always 3 layers: domain + data + presentation
- Do NOT add `core:database` to the presentation module
- Do NOT skip the domain layer — even for simple CRUD always use Repository + UseCase
- Do NOT make a God-UseCase — one UseCase = one operation
- Do NOT use `ViewModelComponent` for repositories — use `SingletonComponent`
- Do NOT create `@Module` without `@InstallIn`
- Do NOT use `kapt` — the project uses KSP
- Do NOT hardcode versions in build.gradle.kts — use `libs.versions.toml`
- Do NOT forget to register all 3 modules in `settings.gradle.kts`
