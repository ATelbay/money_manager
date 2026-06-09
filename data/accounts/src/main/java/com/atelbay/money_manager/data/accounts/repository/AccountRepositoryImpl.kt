package com.atelbay.money_manager.data.accounts.repository

import androidx.room.withTransaction
import com.atelbay.money_manager.core.database.MoneyManagerDatabase
import com.atelbay.money_manager.core.database.dao.AccountDao
import com.atelbay.money_manager.core.database.dao.TransactionDao
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
    private val database: MoneyManagerDatabase,
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
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
            // Editing only touches name/currency. Preserve the *current* DB balance rather than the
            // value carried on `entity` (a stale UI snapshot), otherwise concurrent transaction-driven
            // balance changes would be silently overwritten (lost update). Missing row → no-op.
            val existing = accountDao.getById(entity.id) ?: return entity.id
            accountDao.update(
                entity.copy(
                    balance = existing.balance,
                    createdAt = existing.createdAt,
                    remoteId = existing.remoteId,
                    isDeleted = existing.isDeleted,
                    updatedAt = now,
                ),
            )
            syncManager.syncAccount(entity.id)
            entity.id
        }
    }

    override suspend fun delete(id: Long) {
        val now = System.currentTimeMillis()
        // Capture affected transaction ids before the cascade, soft-delete both atomically, then
        // propagate each transaction's deletion to sync (otherwise they'd linger on other devices).
        val deletedTransactionIds = database.withTransaction {
            val ids = transactionDao.getIdsByAccountId(id)
            transactionDao.softDeleteByAccountId(id, now)
            accountDao.softDeleteById(id, now)
            ids
        }
        deletedTransactionIds.forEach { syncManager.syncTransaction(it) }
        syncManager.syncAccount(id)
    }
}
