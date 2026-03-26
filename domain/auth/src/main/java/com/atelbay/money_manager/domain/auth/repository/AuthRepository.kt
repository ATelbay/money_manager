package com.atelbay.money_manager.domain.auth.repository

import com.atelbay.money_manager.core.auth.AuthUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun observeCurrentUser(): Flow<AuthUser?>
    suspend fun signInWithGoogle(): AuthUser
    suspend fun signOut()

    /**
     * Ensures there is an active Firebase Auth session.
     * If no user is currently signed in, signs in anonymously.
     * No-op if a user (Google or anonymous) is already signed in.
     */
    suspend fun signInAnonymouslyIfNeeded()
}
