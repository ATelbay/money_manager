package com.atelbay.money_manager.data.sync

sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Syncing : SyncStatus
    data class Synced(val lastSyncedAt: Long) : SyncStatus
    data class Failed(val lastSyncedAt: Long?) : SyncStatus
}
