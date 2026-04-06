package com.atelbay.money_manager.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PendingNavigationManager @Inject constructor() {

    private val _pendingAction = MutableStateFlow<NavigationAction?>(null)
    val pendingAction: StateFlow<NavigationAction?> = _pendingAction.asStateFlow()

    private val _launchedFromExternal = MutableStateFlow(false)
    val launchedFromExternal: StateFlow<Boolean> = _launchedFromExternal.asStateFlow()

    fun enqueue(action: NavigationAction) {
        _pendingAction.value = action
    }

    fun consume() {
        _pendingAction.value = null
    }

    fun markExternalLaunch() {
        _launchedFromExternal.value = true
    }

    fun clearExternalLaunch() {
        _launchedFromExternal.value = false
    }
}
