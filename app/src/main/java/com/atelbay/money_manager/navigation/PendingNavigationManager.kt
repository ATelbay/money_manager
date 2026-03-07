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

    fun enqueue(action: NavigationAction) {
        _pendingAction.value = action
    }

    fun consume() {
        _pendingAction.value = null
    }
}
