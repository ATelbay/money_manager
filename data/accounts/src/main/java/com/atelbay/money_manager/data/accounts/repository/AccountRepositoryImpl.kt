package com.atelbay.money_manager.data.accounts.repository

import com.atelbay.money_manager.core.database.dao.AccountDao
import com.atelbay.money_manager.data.accounts.mapper.toDomain
import com.atelbay.money_manager.data.accounts.mapper.toEntity
import com.atelbay.money_manager.core.model.Account
import com.atelbay.money_manager.data.sync.SyncManager
import com.atelbay.money_manager.domain.accounts.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao,
    private val syncManager: SyncManager,
) : AccountRepository {

    override fun observeAll(): Flow<List<Account>> =
        accountDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeById(id: Long): Flow<Account?> =
        accountDao.observeById(id).map { it?.toDomain() }

    override suspend fun save(account: Account): Long {
        val entity = account.toEntity()
        val now = System.currentTimeMillis()
        return if (entity.id == 0L) {
            val id = accountDao.insert(entity.copy(createdAt = now, updatedAt = now))
            syncManager.syncAccount(id)
            id
        } else {
            val existing = accountDao.getById(entity.id)
            accountDao.update(
                entity.copy(
                    remoteId = existing?.remoteId,
                    isDeleted = existing?.isDeleted ?: false,
                    updatedAt = now,
                ),
            )
            syncManager.syncAccount(entity.id)
            entity.id
        }
    }

    override suspend fun delete(id: Long) {
        val now = System.currentTimeMillis()
        accountDao.softDeleteById(id, now)
        syncManager.syncAccount(id)
    }
}
