package com.atelbay.money_manager.data.debts.repository

import com.atelbay.money_manager.core.database.dao.AccountDao
import com.atelbay.money_manager.core.database.dao.DebtDao
import com.atelbay.money_manager.core.database.dao.DebtPaymentDao
import com.atelbay.money_manager.core.model.Debt
import com.atelbay.money_manager.data.debts.mapper.toDomain
import com.atelbay.money_manager.data.debts.mapper.toEntity
import com.atelbay.money_manager.data.sync.SyncManager
import com.atelbay.money_manager.domain.debts.repository.DebtPaymentRepository
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
    private val debtPaymentRepository: DebtPaymentRepository,
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
                        val paid = paidAmounts[index] ?: 0L
                        val account = accountMap[entity.accountId]
                        entity.toDomain(paid, account?.name.orEmpty())
                    }
                }
            }
        }

    override fun observeById(id: Long): Flow<Debt?> =
        combine(
            debtDao.observeById(id),
            debtPaymentDao.sumAmountByDebtId(id),
            accountDao.observeAll(),
        ) { entity, paidAmount, accounts ->
            entity ?: return@combine null
            val accountName = accounts.find { it.id == entity.accountId }?.name.orEmpty()
            entity.toDomain(paidAmount ?: 0L, accountName)
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
        // Delegate so each payment's linked transaction is reversed (balance + soft-delete), not just
        // the payment rows. See DebtPaymentRepositoryImpl.deleteAllByDebtId.
        debtPaymentRepository.deleteAllByDebtId(id)
        debtDao.softDeleteById(id, System.currentTimeMillis())
        syncManager.syncDebt(id)
    }
}
