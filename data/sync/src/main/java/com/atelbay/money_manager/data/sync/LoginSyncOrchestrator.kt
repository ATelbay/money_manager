package com.atelbay.money_manager.data.sync

import com.atelbay.money_manager.core.auth.AuthManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Observes auth state and triggers pull + pending push sync whenever a user signs in.
 * Call [start] once from Application.onCreate.
 */
@Singleton
class LoginSyncOrchestrator @Inject constructor(
    private val authManager: AuthManager,
    private val pullSyncUseCase: PullSyncUseCase,
    private val syncManager: SyncManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start() {
        scope.launch {
            authManager.currentUser
                .distinctUntilChangedBy { it?.userId }
                .collect { user ->
                    if (user != null) {
                        Timber.d("LoginSyncOrchestrator: user signed in (${user.userId}), starting sync")
                        runSync(user.userId)
                    }
                }
        }
    }

    fun retrySync() {
        scope.launch {
            val userId = authManager.currentUser.value?.userId ?: return@launch
            runSync(userId)
        }
    }

    private suspend fun runSync(userId: String) {
        syncManager.updateStatus(SyncStatus.Syncing)
        try {
            pullSyncUseCase(userId)
            syncManager.pushAllPending()
            syncManager.pushAllAccounts()
            syncManager.updateStatus(SyncStatus.Synced(System.currentTimeMillis()))
            Timber.d("LoginSyncOrchestrator: sync complete for userId=$userId")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "LoginSyncOrchestrator: sync failed")
            syncManager.updateStatus(SyncStatus.Failed(syncManager.lastSuccessfulSyncAt))
        }
    }
}
