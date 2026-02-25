package com.atelbay.money_manager.domain.accounts.usecase

import com.atelbay.money_manager.domain.accounts.repository.AccountRepository
import javax.inject.Inject

class DeleteAccountUseCase @Inject constructor(
    private val repository: AccountRepository,
) {
    suspend operator fun invoke(id: Long) = repository.delete(id)
}
