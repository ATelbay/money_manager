package com.atelbay.money_manager.presentation.onboarding.ui

data class OnboardingState(
    val currentPage: Int = 0,
)

/** Single source of truth for the number of onboarding pages (see [localizedOnboardingPages]). */
const val ONBOARDING_PAGE_COUNT = 3

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: String,
)
