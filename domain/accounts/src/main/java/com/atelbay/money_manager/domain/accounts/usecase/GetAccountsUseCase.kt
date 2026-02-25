package com.atelbay.money_manager.domain.accounts.usecase

import com.atelbay.money_manager.core.model.Account
import com.atelbay.money_manager.domain.accounts.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAccountsUseCase @Inject constructor(
    private val repository: AccountRepository,
) {
    operator fun invoke(): Flow<List<Account>> = repository.observeAll()
}
