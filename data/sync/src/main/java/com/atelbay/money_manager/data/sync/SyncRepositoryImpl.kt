package com.atelbay.money_manager.data.sync

import com.atelbay.money_manager.core.model.SyncStatus
import com.atelbay.money_manager.domain.sync.SyncRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

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
