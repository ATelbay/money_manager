package com.atelbay.money_manager.presentation.transactions.ui.list

import com.atelbay.money_manager.core.datastore.UserPreferences
import com.atelbay.money_manager.core.model.Account
import com.atelbay.money_manager.core.model.Transaction
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.domain.accounts.repository.AccountRepository
import com.atelbay.money_manager.domain.accounts.usecase.GetAccountsUseCase
import com.atelbay.money_manager.domain.exchangerate.model.ExchangeRate
import com.atelbay.money_manager.domain.exchangerate.repository.ExchangeRateRepository
import com.atelbay.money_manager.domain.exchangerate.usecase.ConvertAmountUseCase
import com.atelbay.money_manager.domain.exchangerate.usecase.ObserveExchangeRateUseCase
import com.atelbay.money_manager.domain.transactions.repository.TransactionRepository
import com.atelbay.money_manager.domain.transactions.usecase.DeleteTransactionUseCase
import com.atelbay.money_manager.domain.transactions.usecase.GetTransactionsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import com.atelbay.money_manager.core.ui.util.MoneyDisplayMode
import com.atelbay.money_manager.core.ui.util.isUnavailable
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionListViewModelTest {

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state stays loading before debounced filters emit`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val viewModel = createViewModel(
            transactions = listOf(
                transaction(
                    amount = 125.0,
                    type = TransactionType.INCOME,
                    categoryName = "Salary",
                ),
            ),
            accounts = listOf(account(currency = "USD", balance = 125.0)),
            baseCurrency = flowOf("USD"),
        )

        assertEquals(true, viewModel.state.value.isLoading)
        assertEquals(0, viewModel.state.value.transactionRows.size)
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
            accounts = listOf(account(currency = "USD", balance = 125.0)),
            baseCurrency = flowOf("usd"),
        )

        advanceTimeBy(300)
        advanceUntilIdle()

        val row = viewModel.state.value.transactionRows.single()
        assertEquals(125.0, row.displayAmount, 0.0)
        assertEquals("USD", row.displayCurrency)
        assertEquals(ConversionStatus.UNAVAILABLE, row.conversionStatus)
        assertEquals("$", row.displayMoneyDisplay.primaryLabel)
        assertEquals("USD", row.displayMoneyDisplay.secondaryLabel)
        assertFalse(row.displayMoneyDisplay.isUnavailable)
        assertNull(row.secondaryMoneyDisplay)

        assertEquals(125.0, viewModel.state.value.balance ?: 0.0, 0.0)
        assertEquals(125.0, viewModel.state.value.periodIncome ?: 0.0, 0.0)
        assertEquals(0.0, viewModel.state.value.periodExpense ?: 0.0, 0.0)
        assertEquals("$", viewModel.state.value.summaryMoneyDisplay.primaryLabel)
        assertFalse(viewModel.state.value.summaryMoneyDisplay.isUnavailable)
        assertEquals(MoneyDisplayMode.SYMBOL_PLUS_CODE, viewModel.state.value.summaryMoneyDisplay.displayMode)
    }

    @Test
    fun `base currency conversion keeps original context for income and expense rows`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val viewModel = createViewModel(
            transactions = listOf(
                transaction(id = 1L, amount = 47_500.0, type = TransactionType.INCOME, categoryName = "Salary"),
                transaction(id = 2L, amount = 9_500.0, type = TransactionType.EXPENSE, categoryName = "Food"),
            ),
            accounts = listOf(account(currency = "KZT", balance = 57_000.0)),
            baseCurrency = flowOf("USD"),
            exchangeRate = MutableStateFlow(snapshot(mapOf("KZT" to 1.0, "USD" to 475.0))),
        )

        advanceTimeBy(300)
        advanceUntilIdle()

        val rows = viewModel.state.value.transactionRows
        val incomeRow = rows.first { it.transaction.type == TransactionType.INCOME }
        val expenseRow = rows.first { it.transaction.type == TransactionType.EXPENSE }

        assertEquals(100.0, incomeRow.convertedAmount ?: 0.0, 0.0)
        assertEquals("USD", incomeRow.convertedCurrency)
        assertEquals(ConversionStatus.AVAILABLE, incomeRow.conversionStatus)
        assertEquals("$", incomeRow.displayMoneyDisplay.primaryLabel)
        assertEquals("USD", incomeRow.displayMoneyDisplay.secondaryLabel)
        assertFalse(incomeRow.displayMoneyDisplay.isUnavailable)
        assertNotNull(incomeRow.secondaryMoneyDisplay)
        assertEquals("₸", incomeRow.secondaryMoneyDisplay?.primaryLabel)
        assertFalse(incomeRow.secondaryMoneyDisplay?.isUnavailable ?: true)

        assertEquals(20.0, expenseRow.convertedAmount ?: 0.0, 0.0)
        assertEquals("USD", expenseRow.convertedCurrency)
        assertEquals(ConversionStatus.AVAILABLE, expenseRow.conversionStatus)
        assertFalse(expenseRow.displayMoneyDisplay.isUnavailable)
        assertNotNull(expenseRow.secondaryMoneyDisplay)

        assertEquals(120.0, viewModel.state.value.balance ?: 0.0, 0.0)
        assertEquals(100.0, viewModel.state.value.periodIncome ?: 0.0, 0.0)
        assertEquals(20.0, viewModel.state.value.periodExpense ?: 0.0, 0.0)
        assertEquals("$", viewModel.state.value.summaryMoneyDisplay.primaryLabel)
        assertFalse(viewModel.state.value.summaryMoneyDisplay.isUnavailable)
    }

    @Test
    fun `missing exchange rate keeps single currency summary in original currency`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val viewModel = createViewModel(
            transactions = listOf(
                transaction(amount = 47_500.0, type = TransactionType.EXPENSE, categoryName = "Food"),
            ),
            accounts = listOf(account(currency = "KZT", balance = 47_500.0)),
            baseCurrency = flowOf("USD"),
            exchangeRate = MutableStateFlow(null),
        )

        advanceTimeBy(300)
        advanceUntilIdle()

        val row = viewModel.state.value.transactionRows.single()
        assertEquals(47_500.0, row.displayAmount, 0.0)
        assertEquals("KZT", row.displayCurrency)
        assertEquals(ConversionStatus.UNAVAILABLE, row.conversionStatus)
        assertEquals("₸", row.displayMoneyDisplay.primaryLabel)
        assertFalse(row.displayMoneyDisplay.isUnavailable)
        assertNull(row.secondaryMoneyDisplay)

        assertEquals(47_500.0, viewModel.state.value.balance ?: 0.0, 0.0)
        assertEquals(0.0, viewModel.state.value.periodIncome ?: 0.0, 0.0)
        assertEquals(47_500.0, viewModel.state.value.periodExpense ?: 0.0, 0.0)
        assertEquals("₸", viewModel.state.value.summaryMoneyDisplay.primaryLabel)
        assertFalse(viewModel.state.value.summaryMoneyDisplay.isUnavailable)
    }

    @Test
    fun `mixed currency summaries convert into non USD base when all quotes exist`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val viewModel = createViewModel(
            transactions = listOf(
                transaction(id = 1L, amount = 10_400.0, type = TransactionType.EXPENSE, categoryName = "Food", accountId = 1L),
                transaction(id = 2L, amount = 52.0, type = TransactionType.INCOME, categoryName = "Salary", accountId = 2L),
            ),
            accounts = listOf(
                account(id = 1L, currency = "KZT", balance = 52_000.0),
                account(id = 2L, currency = "USD", balance = 100.0),
            ),
            baseCurrency = flowOf("EUR"),
            exchangeRate = MutableStateFlow(
                snapshot(
                    mapOf(
                        "KZT" to 1.0,
                        "USD" to 475.0,
                        "EUR" to 520.0,
                    ),
                ),
            ),
        )

        advanceTimeBy(300)
        advanceUntilIdle()

        assertEquals(191.35, viewModel.state.value.balance ?: 0.0, 0.0)
        assertEquals(47.5, viewModel.state.value.periodIncome ?: 0.0, 0.0)
        assertEquals(20.0, viewModel.state.value.periodExpense ?: 0.0, 0.0)
        assertEquals("€", viewModel.state.value.summaryMoneyDisplay.primaryLabel)
        assertFalse(viewModel.state.value.summaryMoneyDisplay.isUnavailable)
        assertEquals(MoneyDisplayMode.SYMBOL_FIRST, viewModel.state.value.summaryMoneyDisplay.displayMode)
    }

    @Test
    fun `full fallback when one account currency quote is missing shows unavailable summary`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val viewModel = createViewModel(
            transactions = listOf(
                transaction(id = 1L, amount = 47_500.0, type = TransactionType.INCOME, categoryName = "Salary", accountId = 1L),
                transaction(id = 2L, amount = 100.0, type = TransactionType.EXPENSE, categoryName = "Shopping", accountId = 2L),
            ),
            accounts = listOf(
                account(id = 1L, currency = "KZT", balance = 47_500.0),
                account(id = 2L, currency = "GBP", balance = 100.0),
            ),
            baseCurrency = flowOf("USD"),
            exchangeRate = MutableStateFlow(snapshot(mapOf("KZT" to 1.0, "USD" to 475.0))),
        )

        advanceTimeBy(300)
        advanceUntilIdle()

        viewModel.state.value.transactionRows.forEach { row ->
            assertNull(row.convertedAmount)
            assertNull(row.convertedCurrency)
            assertEquals(ConversionStatus.UNAVAILABLE, row.conversionStatus)
            assertFalse(row.displayMoneyDisplay.isUnavailable)
            assertNull(row.secondaryMoneyDisplay)
        }
        assertNull(viewModel.state.value.balance)
        assertNull(viewModel.state.value.periodIncome)
        assertNull(viewModel.state.value.periodExpense)
        assertTrue(viewModel.state.value.summaryMoneyDisplay.isUnavailable)
        assertEquals("-", viewModel.state.value.summaryMoneyDisplay.primaryLabel)
    }

    @Test
    fun `toggleAccountPicker sets showAccountPicker to true`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val viewModel = createViewModel(
            transactions = emptyList(),
            accounts = emptyList(),
            baseCurrency = flowOf("KZT"),
        )

        viewModel.toggleAccountPicker()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.showAccountPicker)
    }

    @Test
    fun `dismissAccountPicker sets showAccountPicker to false`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val viewModel = createViewModel(
            transactions = emptyList(),
            accounts = emptyList(),
            baseCurrency = flowOf("KZT"),
        )

        viewModel.toggleAccountPicker()
        advanceUntilIdle()
        viewModel.dismissAccountPicker()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.showAccountPicker)
    }

    @Test
    fun `selectAccount calls setSelectedAccountId with given id`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val userPreferences = mockk<UserPreferences>()
        every { userPreferences.selectedAccountId } returns flowOf(null)
        every { userPreferences.baseCurrency } returns flowOf("KZT")
        coEvery { userPreferences.setSelectedAccountId(any()) } just Runs

        val viewModel = TransactionListViewModel(
            getTransactionsUseCase = GetTransactionsUseCase(FakeTransactionRepository()),
            deleteTransactionUseCase = DeleteTransactionUseCase(FakeTransactionRepository()),
            getAccountsUseCase = GetAccountsUseCase(FakeAccountRepository()),
            observeExchangeRateUseCase = ObserveExchangeRateUseCase(
                FakeExchangeRateRepository(MutableStateFlow(null)),
            ),
            convertAmountUseCase = ConvertAmountUseCase(),
            userPreferences = userPreferences,
            defaultDispatcher = StandardTestDispatcher(testScheduler),
        )

        viewModel.selectAccount(42L)
        advanceUntilIdle()

        coVerify { userPreferences.setSelectedAccountId(42L) }
    }

    @Test
    fun `computeDailyNetSums groups income and expense by date correctly`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        // Use two timestamps within the last 30 days (MONTH period) to avoid being filtered out
        val now = System.currentTimeMillis()
        val todayMillis = now
        val yesterdayMillis = now - 86_400_000L // 24 hours ago

        val viewModel = createViewModel(
            transactions = listOf(
                transaction(id = 1L, amount = 1000.0, type = TransactionType.INCOME, categoryName = "Salary", date = todayMillis),
                transaction(id = 2L, amount = 300.0, type = TransactionType.EXPENSE, categoryName = "Food", date = todayMillis),
                transaction(id = 3L, amount = 500.0, type = TransactionType.INCOME, categoryName = "Gift", date = yesterdayMillis),
            ),
            accounts = listOf(account(currency = "KZT", balance = 1200.0)),
            baseCurrency = flowOf("KZT"),
            exchangeRate = MutableStateFlow(snapshot(mapOf("KZT" to 1.0, "USD" to 475.0))),
        )

        advanceTimeBy(300)
        advanceUntilIdle()

        val dailyNetSums = viewModel.state.value.dailyNetSums

        val todayKey = java.time.Instant.ofEpochMilli(todayMillis)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
            .toString()
        val yesterdayKey = java.time.Instant.ofEpochMilli(yesterdayMillis)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
            .toString()

        // today: 1000 income - 300 expense = 700
        assertEquals(700.0, dailyNetSums[todayKey] ?: 0.0, 0.01)
        // yesterday: 500 income - 0 expense = 500
        assertEquals(500.0, dailyNetSums[yesterdayKey] ?: 0.0, 0.01)
    }

    @Test
    fun `dailyNetSums is empty when currencies cannot be converted`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val viewModel = createViewModel(
            transactions = listOf(
                transaction(id = 1L, amount = 1000.0, type = TransactionType.INCOME, categoryName = "Salary", accountId = 1L),
                transaction(id = 2L, amount = 50.0, type = TransactionType.EXPENSE, categoryName = "Food", accountId = 2L),
            ),
            accounts = listOf(
                account(id = 1L, currency = "KZT", balance = 1000.0),
                account(id = 2L, currency = "USD", balance = 50.0),
            ),
            baseCurrency = flowOf("KZT"),
            // Exchange rate missing USD → conversion unavailable
            exchangeRate = MutableStateFlow(null),
        )

        advanceTimeBy(300)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.dailyNetSums.isEmpty())
    }

    @Test
    fun `account filtering shows only transactions for selected account`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val viewModel = createViewModel(
            transactions = listOf(
                transaction(id = 1L, amount = 100.0, type = TransactionType.INCOME, categoryName = "Salary", accountId = 1L),
                transaction(id = 2L, amount = 200.0, type = TransactionType.EXPENSE, categoryName = "Food", accountId = 2L),
                transaction(id = 3L, amount = 50.0, type = TransactionType.INCOME, categoryName = "Gift", accountId = 1L),
            ),
            accounts = listOf(
                account(id = 1L, currency = "KZT", balance = 150.0),
                account(id = 2L, currency = "KZT", balance = 800.0),
            ),
            baseCurrency = flowOf("KZT"),
            selectedAccountId = 1L,
        )

        advanceTimeBy(300)
        advanceUntilIdle()

        val rows = viewModel.state.value.transactionRows
        assertTrue(rows.all { it.transaction.accountId == 1L })
        assertEquals(2, rows.size)
    }

    private fun TestScope.createViewModel(
        transactions: List<Transaction>,
        accounts: List<Account>,
        baseCurrency: Flow<String>,
        exchangeRate: MutableStateFlow<ExchangeRate?> = MutableStateFlow(
            snapshot(mapOf("KZT" to 1.0, "USD" to 475.0)),
        ),
        selectedAccountId: Long? = null,
    ): TransactionListViewModel {
        val userPreferences = mockk<UserPreferences>()
        every { userPreferences.selectedAccountId } returns flowOf(selectedAccountId)
        every { userPreferences.baseCurrency } returns baseCurrency
        coEvery { userPreferences.setSelectedAccountId(any()) } just Runs

        return TransactionListViewModel(
            getTransactionsUseCase = GetTransactionsUseCase(FakeTransactionRepository(transactions)),
            deleteTransactionUseCase = DeleteTransactionUseCase(FakeTransactionRepository()),
            getAccountsUseCase = GetAccountsUseCase(FakeAccountRepository(accounts)),
            observeExchangeRateUseCase = ObserveExchangeRateUseCase(
                FakeExchangeRateRepository(exchangeRate),
            ),
            convertAmountUseCase = ConvertAmountUseCase(),
            userPreferences = userPreferences,
            defaultDispatcher = StandardTestDispatcher(testScheduler),
        )
    }

    private fun snapshot(quotes: Map<String, Double>) = ExchangeRate(
        quotes = quotes,
        fetchedAt = 1L,
        source = "NBK",
    )

    private fun transaction(
        id: Long = 1L,
        amount: Double,
        type: TransactionType,
        categoryName: String,
        accountId: Long = 1L,
        date: Long = System.currentTimeMillis(),
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
        date = date,
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

        override fun observeByCategoryTypeAndDateRange(
            categoryId: Long,
            transactionType: TransactionType,
            startMillis: Long,
            endMillis: Long,
        ): Flow<List<Transaction>> = flowOf(
            transactions.filter { transaction ->
                transaction.categoryId == categoryId &&
                    transaction.type == transactionType &&
                    transaction.date in startMillis..endMillis
            },
        )

        override fun observeById(id: Long): Flow<Transaction?> =
            flowOf(transactions.firstOrNull { it.id == id })

        override suspend fun save(transaction: Transaction): Long = transaction.id

        override suspend fun delete(id: Long) = Unit

        override suspend fun getTopCurrenciesByUsage(): List<String> = emptyList()
    }

    private class FakeAccountRepository(
        private val accounts: List<Account> = emptyList(),
    ) : AccountRepository {
        override fun observeAll(): Flow<List<Account>> = flowOf(accounts)

        override fun observeById(id: Long): Flow<Account?> =
            flowOf(accounts.firstOrNull { it.id == id })

        override suspend fun save(account: Account): Long = account.id

        override suspend fun delete(id: Long) = Unit
    }

    private class FakeExchangeRateRepository(
        private val exchangeRate: MutableStateFlow<ExchangeRate?>,
    ) : ExchangeRateRepository {
        override fun observeQuotes(): Flow<ExchangeRate?> = exchangeRate

        override suspend fun saveQuotes(rate: ExchangeRate) = Unit

        override suspend fun fetchAndStoreQuotes(): ExchangeRate = exchangeRate.value
            ?: error("Exchange rate missing")
    }
}
