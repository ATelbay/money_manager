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
import io.mockk.every
import io.mockk.mockk
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        assertNull(viewModel.state.value.displayCurrency)
        assertEquals(SummaryDisplayMode.UNAVAILABLE, viewModel.state.value.summaryDisplayMode)
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

        assertEquals("USD", viewModel.state.value.displayCurrency)
        assertEquals(125.0, viewModel.state.value.balance ?: 0.0, 0.0)
        assertEquals(125.0, viewModel.state.value.periodIncome ?: 0.0, 0.0)
        assertEquals(0.0, viewModel.state.value.periodExpense ?: 0.0, 0.0)
        assertEquals("$", viewModel.state.value.summaryMoneyDisplay.primaryLabel)
        assertEquals(
            SummaryDisplayMode.CONVERTED,
            viewModel.state.value.summaryDisplayMode,
        )
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

        assertEquals(20.0, expenseRow.convertedAmount ?: 0.0, 0.0)
        assertEquals("USD", expenseRow.convertedCurrency)
        assertEquals(ConversionStatus.AVAILABLE, expenseRow.conversionStatus)

        assertEquals("USD", viewModel.state.value.displayCurrency)
        assertEquals(120.0, viewModel.state.value.balance ?: 0.0, 0.0)
        assertEquals(100.0, viewModel.state.value.periodIncome ?: 0.0, 0.0)
        assertEquals(20.0, viewModel.state.value.periodExpense ?: 0.0, 0.0)
        assertEquals(SummaryDisplayMode.CONVERTED, viewModel.state.value.summaryDisplayMode)
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

        assertEquals("KZT", viewModel.state.value.displayCurrency)
        assertEquals(47_500.0, viewModel.state.value.balance ?: 0.0, 0.0)
        assertEquals(0.0, viewModel.state.value.periodIncome ?: 0.0, 0.0)
        assertEquals(47_500.0, viewModel.state.value.periodExpense ?: 0.0, 0.0)
        assertEquals(
            SummaryDisplayMode.ORIGINAL_SINGLE_CURRENCY,
            viewModel.state.value.summaryDisplayMode,
        )
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

        assertEquals("EUR", viewModel.state.value.displayCurrency)
        assertEquals(191.35, viewModel.state.value.balance ?: 0.0, 0.0)
        assertEquals(47.5, viewModel.state.value.periodIncome ?: 0.0, 0.0)
        assertEquals(20.0, viewModel.state.value.periodExpense ?: 0.0, 0.0)
        assertEquals(SummaryDisplayMode.CONVERTED, viewModel.state.value.summaryDisplayMode)
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
        }
        assertNull(viewModel.state.value.balance)
        assertNull(viewModel.state.value.periodIncome)
        assertNull(viewModel.state.value.periodExpense)
        assertNull(viewModel.state.value.displayCurrency)
        assertEquals(SummaryDisplayMode.UNAVAILABLE, viewModel.state.value.summaryDisplayMode)
    }

    private fun TestScope.createViewModel(
        transactions: List<Transaction>,
        accounts: List<Account>,
        baseCurrency: Flow<String>,
        exchangeRate: MutableStateFlow<ExchangeRate?> = MutableStateFlow(
            snapshot(mapOf("KZT" to 1.0, "USD" to 475.0)),
        ),
    ): TransactionListViewModel {
        val userPreferences = mockk<UserPreferences>()
        every { userPreferences.selectedAccountId } returns flowOf(null)
        every { userPreferences.baseCurrency } returns baseCurrency

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
