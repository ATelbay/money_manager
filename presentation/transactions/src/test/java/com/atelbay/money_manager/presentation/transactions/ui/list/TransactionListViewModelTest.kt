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
import com.atelbay.money_manager.domain.exchangerate.usecase.GetUsdKztRateUseCase
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
    fun `base currency updates displayed totals`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val baseCurrency = MutableStateFlow("KZT")
        val exchangeRate = MutableStateFlow<ExchangeRate?>(
            ExchangeRate(usdToKzt = 475.0, fetchedAt = 1L),
        )
        val userPreferences = mockk<UserPreferences>()
        every { userPreferences.selectedAccountId } returns flowOf(null)
        every { userPreferences.baseCurrency } returns baseCurrency

        val viewModel = TransactionListViewModel(
            getTransactionsUseCase = GetTransactionsUseCase(
                FakeTransactionRepository(
                    transactions = listOf(
                        Transaction(
                            id = 1L,
                            amount = 47_500.0,
                            type = TransactionType.INCOME,
                            categoryId = 1L,
                            categoryName = "Salary",
                            categoryIcon = "wallet",
                            categoryColor = 0L,
                            accountId = 1L,
                            note = null,
                            date = 1L,
                            createdAt = 1L,
                        ),
                    ),
                ),
            ),
            deleteTransactionUseCase = DeleteTransactionUseCase(FakeTransactionRepository()),
            getAccountsUseCase = GetAccountsUseCase(
                FakeAccountRepository(
                    accounts = listOf(
                        Account(
                            id = 1L,
                            name = "Cash",
                            currency = "KZT",
                            balance = 47_500.0,
                            createdAt = 1L,
                        ),
                    ),
                ),
            ),
            getUsdKztRateUseCase = GetUsdKztRateUseCase(
                FakeExchangeRateRepository(exchangeRate),
            ),
            convertAmountUseCase = ConvertAmountUseCase(),
            userPreferences = userPreferences,
        )

        advanceTimeBy(300)
        advanceUntilIdle()

        assertEquals("KZT", viewModel.state.value.displayCurrency)
        assertEquals(47_500.0, viewModel.state.value.balance, 0.0)
        assertFalse(viewModel.state.value.isUsingConvertedTotals)
        assertFalse(viewModel.state.value.isUsingFallbackCurrency)

        baseCurrency.value = "USD"
        advanceUntilIdle()

        assertEquals("USD", viewModel.state.value.displayCurrency)
        assertEquals(100.0, viewModel.state.value.balance, 0.0)
        assertEquals(100.0, viewModel.state.value.periodIncome, 0.0)
        assertTrue(viewModel.state.value.isUsingConvertedTotals)
        assertFalse(viewModel.state.value.isUsingFallbackCurrency)
    }

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
        override fun observeRate(): Flow<ExchangeRate?> = exchangeRate

        override suspend fun saveRate(rate: ExchangeRate) = Unit

        override suspend fun fetchAndStoreRate(): ExchangeRate = exchangeRate.value
            ?: error("Exchange rate missing")
    }
}
