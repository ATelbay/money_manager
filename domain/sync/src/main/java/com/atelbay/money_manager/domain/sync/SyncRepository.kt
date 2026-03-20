package com.atelbay.money_manager.domain.sync

import com.atelbay.money_manager.core.model.SyncStatus
import kotlinx.coroutines.flow.Flow

interface SyncRepository {
    val syncStatus: Flow<SyncStatus>
    fun retrySync()
}
