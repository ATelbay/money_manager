package com.atelbay.money_manager.data.auth.repository

import com.atelbay.money_manager.core.auth.AuthManager
import com.atelbay.money_manager.core.auth.AuthUser
import com.atelbay.money_manager.domain.auth.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authManager: AuthManager,
) : AuthRepository {

    override fun observeCurrentUser(): Flow<AuthUser?> = authManager.currentUser

    override suspend fun signInWithGoogle(): AuthUser = authManager.signInWithGoogle()

    override suspend fun signOut() = authManager.signOut()

    override suspend fun signInAnonymouslyIfNeeded() = authManager.signInAnonymouslyIfNeeded()
}
