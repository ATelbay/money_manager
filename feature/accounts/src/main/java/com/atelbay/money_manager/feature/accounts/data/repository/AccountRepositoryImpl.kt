package com.atelbay.money_manager.feature.accounts.data.repository

import com.atelbay.money_manager.core.database.dao.AccountDao
import com.atelbay.money_manager.feature.accounts.data.mapper.toDomain
import com.atelbay.money_manager.feature.accounts.data.mapper.toEntity
import com.atelbay.money_manager.core.model.Account
import com.atelbay.money_manager.feature.accounts.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao,
) : AccountRepository {

    override fun observeAll(): Flow<List<Account>> =
        accountDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeById(id: Long): Flow<Account?> =
        accountDao.observeById(id).map { it?.toDomain() }

    override suspend fun save(account: Account): Long {
        val entity = account.toEntity()
        return if (entity.id == 0L) {
            accountDao.insert(entity.copy(createdAt = System.currentTimeMillis()))
        } else {
            accountDao.update(entity)
            entity.id
        }
    }

    override suspend fun delete(id: Long) {
        val entity = accountDao.getById(id) ?: return
        accountDao.delete(entity)
    }
}
