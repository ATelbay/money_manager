package com.atelbay.money_manager.data.debts.repository

import androidx.room.withTransaction
import com.atelbay.money_manager.core.database.MoneyManagerDatabase
import com.atelbay.money_manager.core.database.dao.AccountDao
import com.atelbay.money_manager.core.database.dao.CategoryDao
import com.atelbay.money_manager.core.database.dao.DebtPaymentDao
import com.atelbay.money_manager.core.database.dao.TransactionDao
import com.atelbay.money_manager.core.database.entity.CategoryEntity
import com.atelbay.money_manager.core.database.entity.DebtPaymentEntity
import com.atelbay.money_manager.core.database.entity.TransactionEntity
import com.atelbay.money_manager.core.model.Debt
import com.atelbay.money_manager.core.model.DebtDirection
import com.atelbay.money_manager.core.model.DebtPayment
import com.atelbay.money_manager.core.model.DebtStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DebtPaymentRepositoryImplTest {

    private val database = mockk<MoneyManagerDatabase>()
    private val debtPaymentDao = mockk<DebtPaymentDao>(relaxed = true)
    private val transactionDao = mockk<TransactionDao>(relaxed = true)
    private val categoryDao = mockk<CategoryDao>(relaxed = true)
    private val accountDao = mockk<AccountDao>(relaxed = true)
    private val syncManager = mockk<com.atelbay.money_manager.data.sync.SyncManager>(relaxed = true)

    private lateinit var repository: DebtPaymentRepositoryImpl

    @Before
    fun setUp() {
        // Run any withTransaction block inline.
        mockkStatic("androidx.room.RoomDatabaseKt")
        // withTransaction is an extension fn → arg[0] is the receiver (database), arg[1] is the block.
        coEvery { database.withTransaction(any<suspend () -> Any>()) } coAnswers {
            secondArg<suspend () -> Any>().invoke()
        }
        repository = DebtPaymentRepositoryImpl(
            database, debtPaymentDao, transactionDao, categoryDao, accountDao, syncManager,
        )
    }

    @Test
    fun `lent payment with transaction credits the account balance`() = runTest {
        coEvery { categoryDao.getByType("income") } returns listOf(
            CategoryEntity(id = 9, name = "Возврат долга", icon = "i", color = 0L, type = "income", isDefault = true),
        )
        coEvery { transactionDao.insert(any()) } returns 100L
        coEvery { debtPaymentDao.insert(any()) } returns 5L

        repository.save(
            payment = payment(amount = 30_000L),
            createTransaction = true,
            debt = debt(DebtDirection.LENT, accountId = 2),
        )

        // income → balance increases by +amount
        coVerify(exactly = 1) { accountDao.updateBalance(2, 30_000L, any()) }
        // payment is linked to the created transaction
        coVerify { debtPaymentDao.insert(match { it.transactionId == 100L }) }
    }

    @Test
    fun `borrowed payment with transaction debits the account balance`() = runTest {
        coEvery { categoryDao.getByType("expense") } returns listOf(
            CategoryEntity(id = 8, name = "Долги", icon = "i", color = 0L, type = "expense", isDefault = true),
        )
        coEvery { transactionDao.insert(any()) } returns 101L
        coEvery { debtPaymentDao.insert(any()) } returns 6L

        repository.save(
            payment = payment(amount = 20_000L),
            createTransaction = true,
            debt = debt(DebtDirection.BORROWED, accountId = 3),
        )

        coVerify(exactly = 1) { accountDao.updateBalance(3, -20_000L, any()) }
    }

    @Test
    fun `deleting a payment reverses the linked transaction and balance`() = runTest {
        coEvery { debtPaymentDao.getById(5) } returns DebtPaymentEntity(
            id = 5, debtId = 1, amount = 30_000L, date = 0L, transactionId = 100L, createdAt = 0L,
        )
        coEvery { transactionDao.getById(100) } returns TransactionEntity(
            id = 100, amount = 30_000L, type = "income", categoryId = 9, accountId = 2,
            note = null, date = 0L, createdAt = 0L,
        )

        repository.delete(5)

        // reverse of an income transaction → subtract from the balance
        coVerify(exactly = 1) { accountDao.updateBalance(2, -30_000L, any()) }
        coVerify(exactly = 1) { transactionDao.softDeleteById(100, any()) }
        coVerify(exactly = 1) { debtPaymentDao.softDeleteById(5, any()) }
    }

    private fun payment(amount: Long) = DebtPayment(
        id = 0, debtId = 1, amount = amount, date = 0L, note = null, createdAt = 0L,
    )

    private fun debt(direction: DebtDirection, accountId: Long) = Debt(
        id = 1,
        contactName = "X",
        direction = direction,
        totalAmount = 100_000L,
        paidAmount = 0L,
        remainingAmount = 100_000L,
        currency = "KZT",
        accountId = accountId,
        createdAt = 0L,
        status = DebtStatus.ACTIVE,
    )
}
