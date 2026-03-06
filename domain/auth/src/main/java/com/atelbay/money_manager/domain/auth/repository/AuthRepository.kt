package com.atelbay.money_manager.domain.auth.repository

import android.content.Context
import com.atelbay.money_manager.core.auth.AuthUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun observeCurrentUser(): Flow<AuthUser?>
    suspend fun signInWithGoogle(activityContext: Context): AuthUser
    suspend fun signOut()
}
