package com.atelbay.money_manager.data.auth.repository

import android.content.Context
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

    override suspend fun signInWithGoogle(activityContext: Context): AuthUser =
        authManager.signInWithGoogle(activityContext)

    override suspend fun signOut() = authManager.signOut()
}
