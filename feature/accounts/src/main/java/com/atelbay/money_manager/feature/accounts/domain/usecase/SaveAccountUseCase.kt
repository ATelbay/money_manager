package com.atelbay.money_manager.feature.accounts.domain.usecase

import com.atelbay.money_manager.core.model.Account
import com.atelbay.money_manager.feature.accounts.domain.repository.AccountRepository
import javax.inject.Inject

class SaveAccountUseCase @Inject constructor(
    private val repository: AccountRepository,
) {
    suspend operator fun invoke(account: Account): Long = repository.save(account)
}
