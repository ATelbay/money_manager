package com.atelbay.money_manager.presentation.onboarding.ui

data class OnboardingSetupState(
    val isSigningIn: Boolean = false,
    val isSyncing: Boolean = false,
    val signInError: String? = null,
)
