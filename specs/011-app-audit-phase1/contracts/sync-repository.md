# Contract: SyncRepository (domain:sync)

## Interface

```kotlin
package com.atelbay.money_manager.domain.sync

import com.atelbay.money_manager.core.model.SyncStatus
import kotlinx.coroutines.flow.Flow

interface SyncRepository {
    val syncStatus: Flow<SyncStatus>
    fun retrySync()
}
```

## SyncStatus (core:model)

```kotlin
package com.atelbay.money_manager.core.model

sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Syncing : SyncStatus
    data class Synced(val lastSyncedAt: Long) : SyncStatus
    data class Failed(val lastSyncedAt: Long?) : SyncStatus
}
```

## Implementation (data:sync)

```kotlin
package com.atelbay.money_manager.data.sync

class SyncRepositoryImpl @Inject constructor(
    private val syncManager: SyncManager,
    private val loginSyncOrchestrator: LoginSyncOrchestrator,
) : SyncRepository {

    override val syncStatus: Flow<SyncStatus>
        get() = syncManager.syncStatus

    override fun retrySync() {
        loginSyncOrchestrator.retrySync()
    }
}
```

## Hilt Binding (data:sync)

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {
    @Binds
    abstract fun bindSyncRepository(impl: SyncRepositoryImpl): SyncRepository
}
```

## Consumer (presentation:settings)

`SettingsViewModel` replaces:
- `SyncManager` → `SyncRepository` (for `syncStatus`)
- `LoginSyncOrchestrator` → `SyncRepository` (for `retrySync()`)

Both concrete classes are replaced by a single `SyncRepository` injection.
