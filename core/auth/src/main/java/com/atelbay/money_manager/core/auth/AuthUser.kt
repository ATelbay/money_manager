package com.atelbay.money_manager.core.auth

data class AuthUser(
    val userId: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
)
