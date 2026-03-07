package com.atelbay.money_manager.navigation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PendingNavigationManager @Inject constructor() {

    private val _pendingActions = Channel<NavigationAction>(Channel.UNLIMITED)
    private val _isReady = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val readyActions = _isReady.flatMapLatest { ready ->
        if (ready) _pendingActions.receiveAsFlow() else emptyFlow()
    }

    fun enqueue(action: NavigationAction) {
        _pendingActions.trySend(action)
    }

    fun setReady(ready: Boolean) {
        _isReady.value = ready
    }
}
