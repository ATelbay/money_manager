package com.atelbay.money_manager.data.debts.repository

import androidx.room.withTransaction
import com.atelbay.money_manager.core.database.MoneyManagerDatabase
import com.atelbay.money_manager.core.database.dao.AccountDao
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
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebtPaymentRepositoryImpl @Inject constructor(
    private val database: MoneyManagerDatabase,
    private val debtPaymentDao: DebtPaymentDao,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao,
    private val syncManager: SyncManager,
) : DebtPaymentRepository {

    override fun observeByDebtId(debtId: Long): Flow<List<DebtPayment>> =
        debtPaymentDao.observeByDebtId(debtId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun save(payment: DebtPayment, createTransaction: Boolean, debt: Debt): Long {
        val now = System.currentTimeMillis()
        var paymentEntity = payment.toEntity()

        // On edit, load the prior row so we can drop its previously linked transaction (avoids
        // double-counting / orphaning when the payment is saved again).
        val existingPayment = if (paymentEntity.id != 0L) debtPaymentDao.getById(paymentEntity.id) else null

        val isLent = debt.direction == DebtDirection.LENT
        val categoryName = if (isLent) "Возврат долга" else "Долги"
        val categoryType = if (isLent) "income" else "expense"
        val description = if (isLent) {
            "Возврат долга: ${debt.contactName}"
        } else {
            "Погашение долга: ${debt.contactName}"
        }

        var newTransactionId: Long? = null
        val affectedAccountIds = mutableSetOf<Long>()

        val savedId = database.withTransaction {
            // Reverse the previously linked transaction (edit path) before creating a new one.
            existingPayment?.transactionId?.let { oldTxId ->
                reverseLinkedTransaction(oldTxId, now)?.let { affectedAccountIds.add(it) }
            }

            if (createTransaction) {
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
                    val delta = if (categoryType == "income") payment.amount else -payment.amount
                    accountDao.updateBalance(debt.accountId, delta, now)
                    affectedAccountIds.add(debt.accountId)
                    newTransactionId = transactionId
                } else {
                    Timber.w(
                        "Debt category '%s' (%s) not found; payment saved without a linked transaction",
                        categoryName,
                        categoryType,
                    )
                }
            }

            paymentEntity = paymentEntity.copy(transactionId = newTransactionId)

            if (paymentEntity.id == 0L) {
                debtPaymentDao.insert(paymentEntity.copy(createdAt = now, updatedAt = now))
            } else {
                debtPaymentDao.update(
                    paymentEntity.copy(
                        createdAt = existingPayment?.createdAt ?: now,
                        remoteId = existingPayment?.remoteId,
                        isDeleted = existingPayment?.isDeleted ?: false,
                        updatedAt = now,
                    ),
                )
                paymentEntity.id
            }
        }

        newTransactionId?.let { syncManager.syncTransaction(it) }
        existingPayment?.transactionId?.let { syncManager.syncTransaction(it) }
        affectedAccountIds.forEach { syncManager.syncAccount(it) }
        syncManager.syncDebtPayment(savedId)
        return savedId
    }

    override suspend fun delete(id: Long) {
        val now = System.currentTimeMillis()
        val payment = debtPaymentDao.getById(id)
        var affectedAccountId: Long? = null
        database.withTransaction {
            payment?.transactionId?.let { affectedAccountId = reverseLinkedTransaction(it, now) }
            debtPaymentDao.softDeleteById(id, now)
        }
        payment?.transactionId?.let { syncManager.syncTransaction(it) }
        affectedAccountId?.let { syncManager.syncAccount(it) }
        syncManager.syncDebtPayment(id)
    }

    override suspend fun deleteAllByDebtId(debtId: Long) {
        val now = System.currentTimeMillis()
        val payments = debtPaymentDao.getByDebtId(debtId)
        val affectedAccountIds = mutableSetOf<Long>()
        database.withTransaction {
            payments.forEach { p ->
                p.transactionId?.let { reverseLinkedTransaction(it, now)?.let(affectedAccountIds::add) }
            }
            debtPaymentDao.softDeleteByDebtId(debtId, now)
        }
        payments.forEach { p -> p.transactionId?.let { syncManager.syncTransaction(it) } }
        affectedAccountIds.forEach { syncManager.syncAccount(it) }
        payments.forEach { syncManager.syncDebtPayment(it.id) }
    }

    /**
     * Reverses a debt-payment-linked transaction's effect on the account balance and soft-deletes it.
     * Must be called inside a DB transaction. Returns the affected accountId for post-commit sync, or
     * null if the transaction is missing/already deleted. Mirrors [TransactionRepositoryImpl.delete].
     */
    private suspend fun reverseLinkedTransaction(transactionId: Long, now: Long): Long? {
        val tx = transactionDao.getById(transactionId) ?: return null
        if (tx.isDeleted) return null
        val delta = if (tx.type == "income") -tx.amount else tx.amount
        accountDao.updateBalance(tx.accountId, delta, now)
        transactionDao.softDeleteById(transactionId, now)
        return tx.accountId
    }
}
