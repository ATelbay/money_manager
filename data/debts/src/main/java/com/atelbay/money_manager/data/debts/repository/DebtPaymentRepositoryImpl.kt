package com.atelbay.money_manager.data.debts.repository

import com.atelbay.money_manager.core.database.dao.CategoryDao
import com.atelbay.money_manager.core.database.dao.DebtPaymentDao
import com.atelbay.money_manager.core.database.dao.TransactionDao
import com.atelbay.money_manager.core.database.entity.TransactionEntity
import com.atelbay.money_manager.core.model.Debt
import com.atelbay.money_manager.core.model.DebtDirection
import com.atelbay.money_manager.core.model.DebtPayment
import com.atelbay.money_manager.data.debts.mapper.toDomain
import com.atelbay.money_manager.data.debts.mapper.toEntity
import com.atelbay.money_manager.data.sync.SyncManager
import com.atelbay.money_manager.domain.debts.repository.DebtPaymentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebtPaymentRepositoryImpl @Inject constructor(
    private val debtPaymentDao: DebtPaymentDao,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val syncManager: SyncManager,
) : DebtPaymentRepository {

    override fun observeByDebtId(debtId: Long): Flow<List<DebtPayment>> =
        debtPaymentDao.observeByDebtId(debtId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun save(payment: DebtPayment, createTransaction: Boolean, debt: Debt): Long {
        val now = System.currentTimeMillis()
        var paymentEntity = payment.toEntity()

        if (createTransaction) {
            val isLent = debt.direction == DebtDirection.LENT
            val categoryName = if (isLent) "Возврат долга" else "Долги"
            val categoryType = if (isLent) "income" else "expense"
            val description = if (isLent) {
                "Возврат долга: ${debt.contactName}"
            } else {
                "Погашение долга: ${debt.contactName}"
            }

            val category = categoryDao.getByType(categoryType).find { it.name == categoryName }
            if (category != null) {
                val transactionEntity = TransactionEntity(
                    amount = payment.amount,
                    type = categoryType,
                    categoryId = category.id,
                    accountId = debt.accountId,
                    note = description,
                    date = payment.date,
                    createdAt = now,
                    updatedAt = now,
                )
                val transactionId = transactionDao.insert(transactionEntity)
                paymentEntity = paymentEntity.copy(transactionId = transactionId)
            }
        }

        val savedId = if (paymentEntity.id == 0L) {
            val newEntity = paymentEntity.copy(createdAt = now, updatedAt = now)
            debtPaymentDao.insert(newEntity)
        } else {
            val existing = debtPaymentDao.getById(paymentEntity.id)
            val updatedEntity = paymentEntity.copy(
                createdAt = existing?.createdAt ?: now,
                remoteId = existing?.remoteId,
                isDeleted = existing?.isDeleted ?: false,
                updatedAt = now,
            )
            debtPaymentDao.update(updatedEntity)
            updatedEntity.id
        }
        syncManager.syncDebtPayment(savedId)
        return savedId
    }

    override suspend fun delete(id: Long) {
        debtPaymentDao.softDeleteById(id, System.currentTimeMillis())
        syncManager.syncDebtPayment(id)
    }

    override suspend fun deleteAllByDebtId(debtId: Long) {
        debtPaymentDao.softDeleteByDebtId(debtId, System.currentTimeMillis())
    }
}
