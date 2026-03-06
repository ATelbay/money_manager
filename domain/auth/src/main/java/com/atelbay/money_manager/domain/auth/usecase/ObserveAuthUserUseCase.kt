package com.atelbay.money_manager.domain.auth.usecase

import com.atelbay.money_manager.core.auth.AuthUser
import com.atelbay.money_manager.domain.auth.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAuthUserUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    operator fun invoke(): Flow<AuthUser?> = repository.observeCurrentUser()
}
