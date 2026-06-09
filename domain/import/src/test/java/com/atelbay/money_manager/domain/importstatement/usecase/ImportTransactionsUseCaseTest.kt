package com.atelbay.money_manager.domain.importstatement.usecase

import androidx.room.withTransaction
import com.atelbay.money_manager.core.database.MoneyManagerDatabase
import com.atelbay.money_manager.core.database.dao.AccountDao
import com.atelbay.money_manager.core.database.dao.CategoryDao
import com.atelbay.money_manager.core.database.dao.TransactionDao
import com.atelbay.money_manager.core.database.entity.CategoryEntity
import com.atelbay.money_manager.core.database.entity.TransactionEntity
import com.atelbay.money_manager.core.model.ParsedTransaction
import com.atelbay.money_manager.core.model.TransactionOverride
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.domain.categories.usecase.SaveCategoryUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ImportTransactionsUseCaseTest {

    private lateinit var database: MoneyManagerDatabase
    private lateinit var transactionDao: TransactionDao
    private lateinit var accountDao: AccountDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var saveCategoryUseCase: SaveCategoryUseCase
    private lateinit var useCase: ImportTransactionsUseCase

    private fun parsedTx(
        amount: Long = 10_000L,
        type: TransactionType = TransactionType.EXPENSE,
        categoryId: Long? = null,
        suggestedCategoryName: String? = "hint",
        pendingCategoryName: String? = null,
        uniqueHash: String = "hash",
    ) = ParsedTransaction(
        date = LocalDate(2026, 1, 1),
        amount = amount,
        type = type,
        details = "details",
        categoryId = categoryId,
        suggestedCategoryName = suggestedCategoryName,
        pendingCategoryName = pendingCategoryName,
        confidence = 1f,
        needsReview = false,
        uniqueHash = uniqueHash,
    )

    private fun category(id: Long, name: String, type: String) =
        CategoryEntity(id = id, name = name, icon = "label", color = 0, type = type, isDefault = true)

    @Before
    fun setUp() {
        database = mockk()
        transactionDao = mockk()
        accountDao = mockk(relaxUnitFun = true)
        categoryDao = mockk()
        saveCategoryUseCase = mockk()

        mockkStatic("androidx.room.RoomDatabaseKt")
        // args holds [block, continuation]; the block is the Function1, the continuation is not.
        coEvery { database.withTransaction(any<suspend () -> Any?>()) } coAnswers {
            @Suppress("UNCHECKED_CAST")
            val block = args.first { it is Function1<*, *> } as suspend () -> Any?
            block()
        }
        coEvery { categoryDao.getByType(any()) } returns emptyList()
        coEvery { transactionDao.getExistingHashes(any()) } returns emptyList()
        coEvery { transactionDao.insertOrIgnore(any()) } returns listOf(1L)

        useCase = ImportTransactionsUseCase(
            database = database,
            transactionDao = transactionDao,
            accountDao = accountDao,
            categoryDao = categoryDao,
            saveCategoryUseCase = saveCategoryUseCase,
        )
    }

    @After
    fun tearDown() {
        unmockkStatic("androidx.room.RoomDatabaseKt")
    }

    @Test
    fun `pending category name is created at import and assigned`() = runTest {
        coEvery { saveCategoryUseCase(any()) } returns 10L
        val slot = slot<List<TransactionEntity>>()
        coEvery { transactionDao.insertOrIgnore(capture(slot)) } returns listOf(1L)

        val result = useCase(
            transactions = listOf(parsedTx(pendingCategoryName = "Кафе")),
            accountId = 7L,
            overrides = emptyMap(),
        )

        assertEquals(1, result)
        coVerify { saveCategoryUseCase(match { it.name == "Кафе" && it.type == TransactionType.EXPENSE }) }
        assertEquals(10L, slot.captured.single().categoryId)
        coVerify { accountDao.updateBalance(7L, -10_000L, any()) }
    }

    @Test
    fun `falls back to Other and never drops a transaction`() = runTest {
        coEvery { saveCategoryUseCase(any()) } returns 99L

        val result = useCase(
            transactions = listOf(parsedTx(suggestedCategoryName = null)),
            accountId = 1L,
            overrides = emptyMap(),
        )

        assertEquals(1, result)
        coVerify { saveCategoryUseCase(match { it.name == "Другое" }) }
    }

    @Test
    fun `existing fallback category is reused, not recreated`() = runTest {
        coEvery { categoryDao.getByType("expense") } returns listOf(category(5L, "Другое", "expense"))
        val slot = slot<List<TransactionEntity>>()
        coEvery { transactionDao.insertOrIgnore(capture(slot)) } returns listOf(1L)

        useCase(
            transactions = listOf(parsedTx(suggestedCategoryName = null)),
            accountId = 1L,
            overrides = emptyMap(),
        )

        coVerify(exactly = 0) { saveCategoryUseCase(any()) }
        assertEquals(5L, slot.captured.single().categoryId)
    }

    @Test
    fun `duplicate hashes are skipped and balance not changed`() = runTest {
        coEvery { categoryDao.getByType("expense") } returns listOf(category(5L, "Другое", "expense"))
        coEvery { transactionDao.getExistingHashes(any()) } returns listOf("dup")
        coEvery { transactionDao.insertOrIgnore(any()) } returns emptyList()

        val result = useCase(
            transactions = listOf(parsedTx(uniqueHash = "dup", suggestedCategoryName = null)),
            accountId = 1L,
            overrides = emptyMap(),
        )

        assertEquals(0, result)
        coVerify(exactly = 0) { accountDao.updateBalance(any(), any(), any()) }
    }

    @Test
    fun `override category and type win, income increases balance`() = runTest {
        val slot = slot<List<TransactionEntity>>()
        coEvery { transactionDao.insertOrIgnore(capture(slot)) } returns listOf(1L)

        val result = useCase(
            transactions = listOf(parsedTx(pendingCategoryName = "X")),
            accountId = 3L,
            overrides = mapOf(
                0 to TransactionOverride(
                    categoryId = 42L,
                    type = TransactionType.INCOME,
                    amount = 25_000L,
                ),
            ),
        )

        assertEquals(1, result)
        coVerify(exactly = 0) { saveCategoryUseCase(any()) }
        val entity = slot.captured.single()
        assertEquals(42L, entity.categoryId)
        assertEquals("income", entity.type)
        coVerify { accountDao.updateBalance(3L, 25_000L, any()) }
    }
}
