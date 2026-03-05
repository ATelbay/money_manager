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
        assertEquals(TransactionType.INCOME, row.transaction.type)
        assertEquals(ConversionStatus.UNAVAILABLE, row.conversionStatus)

        assertEquals("USD", viewModel.state.value.displayCurrency)
        assertEquals(125.0, viewModel.state.value.balance, 0.0)
        assertEquals(125.0, viewModel.state.value.periodIncome, 0.0)
        assertEquals(0.0, viewModel.state.value.periodExpense, 0.0)
        assertFalse(viewModel.state.value.isUsingConvertedTotals)
        assertFalse(viewModel.state.value.isUsingFallbackCurrency)
    }

    @Test
    fun `base currency conversion keeps original context for income and expense rows`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val viewModel = createViewModel(
            transactions = listOf(
                transaction(
                    id = 1L,
                    amount = 47_500.0,
                    type = TransactionType.INCOME,
                    categoryName = "Salary",
                ),
                transaction(
                    id = 2L,
                    amount = 9_500.0,
                    type = TransactionType.EXPENSE,
                    categoryName = "Food",
                ),
            ),
            accounts = listOf(
                account(
                    currency = "KZT",
                    balance = 57_000.0,
                ),
            ),
            baseCurrency = flowOf("USD"),
            exchangeRate = MutableStateFlow(
                ExchangeRate(quotes = mapOf("USD" to 475.0), fetchedAt = 1L),
            ),
        )

        advanceTimeBy(300)
        advanceUntilIdle()

        val rows = viewModel.state.value.transactionRows
        val incomeRow = rows.first { it.transaction.type == TransactionType.INCOME }
        val expenseRow = rows.first { it.transaction.type == TransactionType.EXPENSE }

        assertEquals(47_500.0, incomeRow.originalAmount, 0.0)
        assertEquals("KZT", incomeRow.originalCurrency)
        assertTrue(incomeRow.convertedAmount != null)
        assertEquals(100.0, incomeRow.convertedAmount ?: 0.0, 0.0)
        assertEquals("USD", incomeRow.convertedCurrency)
        assertEquals(100.0, incomeRow.displayAmount, 0.0)
        assertEquals("USD", incomeRow.displayCurrency)
        assertEquals(TransactionType.INCOME, incomeRow.transaction.type)
        assertEquals(ConversionStatus.AVAILABLE, incomeRow.conversionStatus)

        assertEquals(9_500.0, expenseRow.originalAmount, 0.0)
        assertEquals("KZT", expenseRow.originalCurrency)
        assertTrue(expenseRow.convertedAmount != null)
        assertEquals(20.0, expenseRow.convertedAmount ?: 0.0, 0.0)
        assertEquals("USD", expenseRow.convertedCurrency)
        assertEquals(20.0, expenseRow.displayAmount, 0.0)
        assertEquals("USD", expenseRow.displayCurrency)
        assertEquals(TransactionType.EXPENSE, expenseRow.transaction.type)
        assertEquals(ConversionStatus.AVAILABLE, expenseRow.conversionStatus)

        assertEquals("USD", viewModel.state.value.displayCurrency)
        assertEquals(120.0, viewModel.state.value.balance, 0.0)
        assertEquals(100.0, viewModel.state.value.periodIncome, 0.0)
        assertEquals(20.0, viewModel.state.value.periodExpense, 0.0)
        assertTrue(viewModel.state.value.isUsingConvertedTotals)
        assertFalse(viewModel.state.value.isUsingFallbackCurrency)
    }

    @Test
    fun `missing exchange rate keeps transaction row in source currency`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val viewModel = createViewModel(
            transactions = listOf(
                transaction(
                    amount = 47_500.0,
                    type = TransactionType.EXPENSE,
                    categoryName = "Food",
                ),
            ),
            accounts = listOf(
                account(
                    currency = "KZT",
                    balance = 47_500.0,
                ),
            ),
            baseCurrency = flowOf("USD"),
            exchangeRate = MutableStateFlow(null),
        )

        advanceTimeBy(300)
        advanceUntilIdle()

        val row = viewModel.state.value.transactionRows.single()
        assertEquals(47_500.0, row.originalAmount, 0.0)
        assertEquals("KZT", row.originalCurrency)
        assertEquals(null, row.convertedAmount)
        assertEquals(null, row.convertedCurrency)
        assertEquals(47_500.0, row.displayAmount, 0.0)
        assertEquals("KZT", row.displayCurrency)
        assertEquals(TransactionType.EXPENSE, row.transaction.type)
        assertEquals(ConversionStatus.UNAVAILABLE, row.conversionStatus)

        assertEquals("KZT", viewModel.state.value.displayCurrency)
        assertEquals(0.0, viewModel.state.value.periodIncome, 0.0)
        assertEquals(47_500.0, viewModel.state.value.periodExpense, 0.0)
        assertFalse(viewModel.state.value.isUsingConvertedTotals)
        assertTrue(viewModel.state.value.isUsingFallbackCurrency)
    }

    private fun createViewModel(
        transactions: List<Transaction>,
        accounts: List<Account>,
        baseCurrency: Flow<String>,
        exchangeRate: MutableStateFlow<ExchangeRate?> = MutableStateFlow(
            ExchangeRate(quotes = mapOf("USD" to 475.0), fetchedAt = 1L),
        ),
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
            observeExchangeRateUseCase = ObserveExchangeRateUseCase(
                FakeExchangeRateRepository(exchangeRate),
            ),
            convertAmountUseCase = ConvertAmountUseCase(),
            userPreferences = userPreferences,
        )
    }

    private fun transaction(
        id: Long = 1L,
        amount: Double,
        type: TransactionType,
        categoryName: String,
    ) = Transaction(
        id = id,
        amount = amount,
        type = type,
        categoryId = 1L,
        categoryName = categoryName,
        categoryIcon = "wallet",
        categoryColor = 0L,
        accountId = 1L,
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
        private val exchangeRate: MutableStateFlow<ExchangeRate?>,
    ) : ExchangeRateRepository {
        override fun observeQuotes(): Flow<ExchangeRate?> = exchangeRate

        override suspend fun saveQuotes(rate: ExchangeRate) = Unit

        override suspend fun fetchAndStoreQuotes(): ExchangeRate = exchangeRate.value
            ?: error("Exchange rate missing")
    }
}
