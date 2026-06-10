package com.atelbay.money_manager.core.auth

data class AuthUser(
    val userId: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    /** True for an anonymous Firebase session (no Google/email account linked). */
    val isAnonymous: Boolean = false,
)
