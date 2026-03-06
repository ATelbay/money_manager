package com.atelbay.money_manager.domain.auth.usecase

import android.content.Context
import com.atelbay.money_manager.core.auth.AuthUser
import com.atelbay.money_manager.domain.auth.repository.AuthRepository
import javax.inject.Inject

class SignInWithGoogleUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(activityContext: Context): AuthUser =
        repository.signInWithGoogle(activityContext)
}
