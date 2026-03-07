package com.atelbay.money_manager.navigation

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PendingNavigationManager @Inject constructor() {

    private val _pendingActions = Channel<NavigationAction>(Channel.UNLIMITED)
    private val _isReady = MutableStateFlow(false)

    /**
     * Emits actions one-by-one, waiting for the ready gate before each delivery.
     * Safer than flatMapLatest + receiveAsFlow which can drop items during gate transitions.
     */
    val readyActions: Flow<NavigationAction> = flow {
        for (action in _pendingActions) {
            _isReady.first { it } // suspend until ready
            emit(action)
        }
    }

    fun enqueue(action: NavigationAction) {
        _pendingActions.trySend(action)
    }

    fun setReady(ready: Boolean) {
        _isReady.value = ready
    }
}
