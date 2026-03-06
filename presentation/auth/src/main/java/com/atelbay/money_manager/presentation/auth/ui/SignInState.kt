package com.atelbay.money_manager.presentation.auth.ui

import com.atelbay.money_manager.core.auth.AuthUser

data class SignInState(
    val isLoading: Boolean = false,
    val user: AuthUser? = null,
    val errorMessage: String? = null,
)
