package com.atelbay.money_manager.domain.auth.repository

import com.atelbay.money_manager.core.auth.AuthUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun observeCurrentUser(): Flow<AuthUser?>
    suspend fun signInWithGoogle(): AuthUser
    suspend fun signOut()
}
