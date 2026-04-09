package com.atelbay.money_manager.data.debts.repository

import com.atelbay.money_manager.core.database.dao.AccountDao
import com.atelbay.money_manager.core.database.dao.DebtDao
import com.atelbay.money_manager.core.database.dao.DebtPaymentDao
import com.atelbay.money_manager.core.model.Debt
import com.atelbay.money_manager.data.debts.mapper.toDomain
import com.atelbay.money_manager.data.debts.mapper.toEntity
import com.atelbay.money_manager.data.sync.SyncManager
import com.atelbay.money_manager.domain.debts.repository.DebtRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebtRepositoryImpl @Inject constructor(
    private val debtDao: DebtDao,
    private val debtPaymentDao: DebtPaymentDao,
    private val accountDao: AccountDao,
    private val syncManager: SyncManager,
) : DebtRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAll(): Flow<List<Debt>> =
        combine(
            debtDao.observeAll(),
            accountDao.observeAll(),
        ) { debts, accounts ->
            debts to accounts.associateBy { it.id }
        }.flatMapLatest { (debts, accountMap) ->
            if (debts.isEmpty()) {
                flowOf(emptyList())
            } else {
                val paidFlows = debts.map { debtPaymentDao.sumAmountByDebtId(it.id) }
                combine(paidFlows) { paidAmounts ->
                    debts.mapIndexed { index, entity ->
                        val paid = paidAmounts[index] ?: 0.0
                        val account = accountMap[entity.accountId]
                        entity.toDomain(paid, account?.name.orEmpty())
                    }
                }
            }
        }

    override fun observeById(id: Long): Flow<Debt?> =
        combine(
            debtDao.observeAll(),
            debtPaymentDao.sumAmountByDebtId(id),
            accountDao.observeAll(),
        ) { debts, paidAmount, accounts ->
            val entity = debts.find { it.id == id } ?: return@combine null
            val accountName = accounts.find { it.id == entity.accountId }?.name.orEmpty()
            entity.toDomain(paidAmount ?: 0.0, accountName)
        }

    override suspend fun getById(id: Long): Debt? {
        val entity = debtDao.getById(id) ?: return null
        val payments = debtPaymentDao.getByDebtId(id)
        val paidAmount = payments.sumOf { it.amount }
        val account = accountDao.getById(entity.accountId)
        return entity.toDomain(paidAmount, account?.name.orEmpty())
    }

    override suspend fun save(debt: Debt): Long {
        val now = System.currentTimeMillis()
        val baseEntity = debt.toEntity()
        val savedId = if (baseEntity.id == 0L) {
            val newEntity = baseEntity.copy(createdAt = now, updatedAt = now)
            debtDao.insert(newEntity)
        } else {
            val existing = debtDao.getById(baseEntity.id)
            val updatedEntity = baseEntity.copy(
                createdAt = existing?.createdAt ?: now,
                remoteId = existing?.remoteId,
                isDeleted = existing?.isDeleted ?: false,
                updatedAt = now,
            )
            debtDao.update(updatedEntity)
            updatedEntity.id
        }
        syncManager.syncDebt(savedId)
        return savedId
    }

    override suspend fun delete(id: Long) {
        debtDao.softDeleteById(id, System.currentTimeMillis())
        syncManager.syncDebt(id)
    }
}
