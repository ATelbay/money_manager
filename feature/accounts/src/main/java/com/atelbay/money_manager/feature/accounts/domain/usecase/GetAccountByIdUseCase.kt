package com.atelbay.money_manager.feature.accounts.domain.usecase

import com.atelbay.money_manager.core.model.Account
import com.atelbay.money_manager.feature.accounts.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAccountByIdUseCase @Inject constructor(
    private val repository: AccountRepository,
) {
    operator fun invoke(id: Long): Flow<Account?> = repository.observeById(id)
}
