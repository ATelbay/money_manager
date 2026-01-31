package com.atelbay.money_manager.feature.accounts.domain.repository

import com.atelbay.money_manager.core.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun observeAll(): Flow<List<Account>>
    fun observeById(id: Long): Flow<Account?>
    suspend fun save(account: Account): Long
    suspend fun delete(id: Long)
}
