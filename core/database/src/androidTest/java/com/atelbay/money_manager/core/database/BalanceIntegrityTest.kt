package com.atelbay.money_manager.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.atelbay.money_manager.core.database.entity.AccountEntity
import com.atelbay.money_manager.core.database.entity.CategoryEntity
import com.atelbay.money_manager.core.database.entity.TransactionEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Penny-drift regression (SC-004 / FR-013): with money stored as [Long] minor units, the
 * materialized account balance — accumulated incrementally through [AccountDao.updateBalance] —
 * must stay exactly equal to the signed SUM of its transactions, with zero drift, even over a long
 * series of fractional-minor amounts (the case that drifted under floating-point storage).
 */
@RunWith(AndroidJUnit4::class)
class BalanceIntegrityTest {

    private lateinit var db: MoneyManagerDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MoneyManagerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun materializedBalanceEqualsSignedSumWithNoDrift() = runBlocking {
        val accountDao = db.accountDao()
        val categoryDao = db.categoryDao()
        val transactionDao = db.transactionDao()

        val accountId = accountDao.insert(
            AccountEntity(name = "Cash", currency = "KZT", balance = 0L, createdAt = 0),
        )
        val categoryId = categoryDao.insert(
            CategoryEntity(name = "Test", icon = "wallet", color = 0L, type = "expense", isDefault = false),
        )

        // 2_000 alternating fractional-minor amounts: +0.10 (10) and -0.20 (20). Under Double
        // accumulation this classically drifts; under Long it must be exact.
        var expected = 0L
        repeat(2_000) { i ->
            val amount = if (i % 2 == 0) 10L else 20L
            val type = if (i % 2 == 0) "income" else "expense"
            transactionDao.insert(
                TransactionEntity(
                    amount = amount,
                    type = type,
                    categoryId = categoryId,
                    accountId = accountId,
                    note = null,
                    date = 0,
                    createdAt = 0,
                ),
            )
            val delta = if (type == "income") amount else -amount
            accountDao.updateBalance(accountId, delta, 0)
            expected += delta
        }

        val storedBalance = accountDao.getById(accountId)!!.balance
        assertEquals("Materialized balance must match the running signed sum", expected, storedBalance)

        // FR-013: balance == SUM(signed transaction amounts) computed directly from the table.
        val dbSignedSum = db.query(
            "SELECT COALESCE(SUM(CASE WHEN type = 'income' THEN amount ELSE -amount END), 0) " +
                "FROM transactions WHERE accountId = ? AND isDeleted = 0",
            arrayOf<Any>(accountId),
        ).use { c ->
            c.moveToFirst()
            c.getLong(0)
        }
        assertEquals("balance must equal SUM(signed amounts) per account (FR-013)", storedBalance, dbSignedSum)
    }
}
