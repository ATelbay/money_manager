package com.atelbay.money_manager.data.sync

import com.atelbay.money_manager.core.auth.AuthManager
import com.atelbay.money_manager.core.datastore.UserPreferences
import com.atelbay.money_manager.core.model.SyncStatus
import com.atelbay.money_manager.core.crypto.FieldCipherHolder
import com.google.firebase.crashlytics.FirebaseCrashlytics
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
 * Observes auth state and triggers pull + pending push sync whenever a real (non-anonymous) user
 * signs in. Call [start] once from Application.onCreate.
 *
 * User isolation: the uid that owns the local data set is tracked in [UserPreferences]. When a
 * *different* uid signs in, local data is wiped first ([SyncManager.clearLocalUserData]) so it can
 * never leak into the new user's Firestore path. On plain sign-out, local data and remoteIds are
 * kept so the same user signing back in does not create duplicate remote documents.
 */
@Singleton
class LoginSyncOrchestrator @Inject constructor(
    private val authManager: AuthManager,
    private val fieldCipherHolder: FieldCipherHolder,
    private val pullSyncUseCase: PullSyncUseCase,
    private val syncManager: SyncManager,
    private val userPreferences: UserPreferences,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start() {
        scope.launch {
            authManager.currentUser
                .distinctUntilChangedBy { it?.userId }
                .collect { user ->
                    when {
                        user == null -> {
                            FirebaseCrashlytics.getInstance().setUserId("")
                            Timber.d("LoginSyncOrchestrator: user signed out")
                            fieldCipherHolder.clear()
                            // Keep local data/remoteIds for a same-user re-login; just stop in-flight work.
                            syncManager.cancelInFlight()
                        }
                        user.isAnonymous -> {
                            // Anonymous sessions (used only for the shared parser-candidate feature)
                            // must never back up the device's personal finance data to the cloud.
                            Timber.d("LoginSyncOrchestrator: anonymous session — skipping cloud sync")
                        }
                        else -> {
                            FirebaseCrashlytics.getInstance().setUserId(user.userId)
                            val previousOwner = userPreferences.getSyncOwnerUid()
                            if (previousOwner != null && previousOwner != user.userId) {
                                Timber.w("LoginSyncOrchestrator: user switch ($previousOwner -> ${user.userId}); wiping local data")
                                syncManager.clearLocalUserData()
                            }
                            userPreferences.setSyncOwnerUid(user.userId)
                            Timber.d("LoginSyncOrchestrator: user signed in (${user.userId}), starting sync")
                            fieldCipherHolder.init(user.userId)
                            runSync(user.userId)
                        }
                    }
                }
        }
    }

    fun retrySync() {
        scope.launch {
            val user = authManager.currentUser.value ?: return@launch
            if (user.isAnonymous) return@launch
            if (fieldCipherHolder.cipher == null) fieldCipherHolder.init(user.userId)
            runSync(user.userId)
        }
    }

    private suspend fun runSync(userId: String) {
        syncManager.updateStatus(SyncStatus.Syncing)
        try {
            syncManager.pushAllPending()
            pullSyncUseCase(userId)
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
