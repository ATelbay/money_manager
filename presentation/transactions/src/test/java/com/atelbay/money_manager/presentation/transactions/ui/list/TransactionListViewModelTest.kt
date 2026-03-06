package com.atelbay.money_manager.presentation.transactions.ui.list

import com.atelbay.money_manager.core.datastore.UserPreferences
import com.atelbay.money_manager.core.model.Account
import com.atelbay.money_manager.core.model.Transaction
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.domain.accounts.repository.AccountRepository
import com.atelbay.money_manager.domain.accounts.usecase.GetAccountsUseCase
import com.atelbay.money_manager.domain.exchangerate.model.CurrencyRate
import com.atelbay.money_manager.domain.exchangerate.model.ExchangeRateSnapshot
import com.atelbay.money_manager.domain.exchangerate.repository.ExchangeRateRepository
import com.atelbay.money_manager.domain.exchangerate.usecase.ConvertAmountUseCase
import com.atelbay.money_manager.domain.exchangerate.usecase.GetExchangeRateSnapshotUseCase
import com.atelbay.money_manager.domain.transactions.repository.TransactionRepository
import com.atelbay.money_manager.domain.transactions.usecase.DeleteTransactionUseCase
import com.atelbay.money_manager.domain.transactions.usecase.GetTransactionsUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionListViewModelTest {

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `same currency keeps transaction row on source amount`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val viewModel = createViewModel(
            transactions = listOf(
                transaction(
                    amount = 125.0,
                    type = TransactionType.INCOME,
                    categoryName = "Salary",
                ),
            ),
            accounts = listOf(
                account(
                    currency = "USD",
                    balance = 125.0,
                ),
            ),
            baseCurrency = flowOf("usd"),
            snapshot = MutableStateFlow(null),
        )

        advanceTimeBy(300)
        advanceUntilIdle()

        val row = viewModel.state.value.transactionRows.single()
        assertEquals(125.0, row.originalAmount, 0.0)
        assertEquals("USD", row.originalCurrency)
        assertEquals(null, row.convertedAmount)
        assertEquals(null, row.convertedCurrency)
        assertEquals(125.0, row.displayAmount, 0.0)
        assertEquals("USD", row.displayCurrency)
        assertEquals(ConversionStatus.UNAVAILABLE, row.conversionStatus)

        assertEquals("USD", viewModel.state.value.displayCurrency)
        assertEquals(125.0, viewModel.state.value.balance, 0.0)
        assertEquals(125.0, viewModel.state.value.periodIncome, 0.0)
        assertEquals(0.0, viewModel.state.value.periodExpense, 0.0)
        assertFalse(viewModel.state.value.isUsingConvertedTotals)
        assertFalse(viewModel.state.value.isUsingFallbackCurrency)
    }

    @Test
    fun `mixed currencies convert into selected base currency when snapshot supports them`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val viewModel = createViewModel(
            transactions = listOf(
                transaction(
                    id = 1L,
                    amount = 47_500.0,
                    type = TransactionType.INCOME,
                    categoryName = "Salary",
                    accountId = 1L,
                ),
                transaction(
                    id = 2L,
                    amount = 10.0,
                    type = TransactionType.EXPENSE,
                    categoryName = "Travel",
                    accountId = 2L,
                ),
            ),
            accounts = listOf(
                account(id = 1L, currency = "KZT", balance = 47_500.0),
                account(id = 2L, currency = "EUR", balance = 100.0),
            ),
            baseCurrency = flowOf("USD"),
            snapshot = MutableStateFlow(snapshot()),
        )

        advanceTimeBy(300)
        advanceUntilIdle()

        val rows = viewModel.state.value.transactionRows
        val kztRow = rows.first { it.transaction.id == 1L }
        val eurRow = rows.first { it.transaction.id == 2L }

        assertEquals(95.0, kztRow.convertedAmount ?: 0.0, 0.0)
        assertEquals("USD", kztRow.displayCurrency)
        assertEquals(11.0, eurRow.convertedAmount ?: 0.0, 0.0)
        assertEquals("USD", eurRow.displayCurrency)

        assertEquals("USD", viewModel.state.value.displayCurrency)
        assertEquals(205.0, viewModel.state.value.balance, 0.0)
        assertEquals(95.0, viewModel.state.value.periodIncome, 0.0)
        assertEquals(11.0, viewModel.state.value.periodExpense, 0.0)
        assertTrue(viewModel.state.value.isUsingConvertedTotals)
        assertFalse(viewModel.state.value.isUsingFallbackCurrency)
    }

    @Test
    fun `missing currency rate keeps summary in fallback mode without mislabeling selected base`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val viewModel = createViewModel(
            transactions = listOf(
                transaction(
                    id = 1L,
                    amount = 47_500.0,
                    type = TransactionType.EXPENSE,
                    categoryName = "Food",
                    accountId = 1L,
                ),
                transaction(
                    id = 2L,
                    amount = 20.0,
                    type = TransactionType.EXPENSE,
                    categoryName = "Books",
                    accountId = 2L,
                ),
            ),
            accounts = listOf(
                account(id = 1L, currency = "KZT", balance = 47_500.0),
                account(id = 2L, currency = "GBP", balance = 20.0),
            ),
            baseCurrency = flowOf("USD"),
            snapshot = MutableStateFlow(
                snapshot().copy(
                    rates = snapshot().rates.filterKeys { it != "GBP" },
                ),
            ),
        )

        advanceTimeBy(300)
        advanceUntilIdle()

        assertEquals("KZT", viewModel.state.value.displayCurrency)
        assertEquals(47_520.0, viewModel.state.value.balance, 0.0)
        assertTrue(viewModel.state.value.isUsingFallbackCurrency)
        assertFalse(viewModel.state.value.isUsingConvertedTotals)

        val rows = viewModel.state.value.transactionRows
        val kztRow = rows.first { it.transaction.id == 1L }
        val gbpRow = rows.first { it.transaction.id == 2L }
        assertEquals(95.0, kztRow.convertedAmount ?: 0.0, 0.0)
        assertEquals(null, gbpRow.convertedAmount)
        assertEquals("GBP", gbpRow.displayCurrency)
    }

    private fun createViewModel(
        transactions: List<Transaction>,
        accounts: List<Account>,
        baseCurrency: Flow<String>,
        snapshot: MutableStateFlow<ExchangeRateSnapshot?>,
    ): TransactionListViewModel {
        val userPreferences = mockk<UserPreferences>()
        every { userPreferences.selectedAccountId } returns flowOf(null)
        every { userPreferences.baseCurrency } returns baseCurrency

        return TransactionListViewModel(
            getTransactionsUseCase = GetTransactionsUseCase(
                FakeTransactionRepository(transactions),
            ),
            deleteTransactionUseCase = DeleteTransactionUseCase(FakeTransactionRepository()),
            getAccountsUseCase = GetAccountsUseCase(
                FakeAccountRepository(accounts),
            ),
            getExchangeRateSnapshotUseCase = GetExchangeRateSnapshotUseCase(
                FakeExchangeRateRepository(snapshot),
            ),
            convertAmountUseCase = ConvertAmountUseCase(),
            userPreferences = userPreferences,
        )
    }

    private fun snapshot() = ExchangeRateSnapshot(
        fetchedAt = 1L,
        source = "NBK",
        rates = mapOf(
            "KZT" to CurrencyRate(code = "KZT", kztPerUnit = 1.0),
            "USD" to CurrencyRate(code = "USD", kztPerUnit = 500.0),
            "EUR" to CurrencyRate(code = "EUR", kztPerUnit = 550.0),
        ),
    )

    private fun transaction(
        id: Long = 1L,
        amount: Double,
        type: TransactionType,
        categoryName: String,
        accountId: Long = 1L,
    ) = Transaction(
        id = id,
        amount = amount,
        type = type,
        categoryId = 1L,
        categoryName = categoryName,
        categoryIcon = "wallet",
        categoryColor = 0L,
        accountId = accountId,
        note = null,
        date = 1L,
        createdAt = 1L,
    )

    private fun account(
        id: Long = 1L,
        currency: String,
        balance: Double,
    ) = Account(
        id = id,
        name = "Cash",
        currency = currency,
        balance = balance,
        createdAt = 1L,
    )

    private class FakeTransactionRepository(
        private val transactions: List<Transaction> = emptyList(),
    ) : TransactionRepository {
        override fun observeAll(): Flow<List<Transaction>> = flowOf(transactions)

        override fun observeById(id: Long): Flow<Transaction?> = flowOf(
            transactions.firstOrNull { it.id == id },
        )

        override suspend fun save(transaction: Transaction): Long = transaction.id

        override suspend fun delete(id: Long) = Unit
    }

    private class FakeAccountRepository(
        private val accounts: List<Account> = emptyList(),
    ) : AccountRepository {
        override fun observeAll(): Flow<List<Account>> = flowOf(accounts)

        override fun observeById(id: Long): Flow<Account?> = flowOf(
            accounts.firstOrNull { it.id == id },
        )

        override suspend fun save(account: Account): Long = account.id

        override suspend fun delete(id: Long) = Unit
    }

    private class FakeExchangeRateRepository(
        private val snapshot: MutableStateFlow<ExchangeRateSnapshot?>,
    ) : ExchangeRateRepository {
        override fun observeRates(): Flow<ExchangeRateSnapshot?> = snapshot

        override suspend fun saveRates(snapshot: ExchangeRateSnapshot) = Unit

        override suspend fun fetchAndStoreRates(): ExchangeRateSnapshot = snapshot.value
            ?: error("Exchange rate snapshot missing")
    }
}
