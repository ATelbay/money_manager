package com.atelbay.money_manager.presentation.settings.ui

import android.app.Application
import com.atelbay.money_manager.core.datastore.UserPreferences
import com.atelbay.money_manager.domain.exchangerate.model.ExchangeRate
import com.atelbay.money_manager.domain.exchangerate.repository.ExchangeRateRepository
import com.atelbay.money_manager.domain.exchangerate.usecase.ObserveExchangeRateUseCase
import com.atelbay.money_manager.domain.transactions.repository.TransactionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `successful refresh resets failure counter`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val userPreferences = mockUserPreferences()
        coEvery { userPreferences.resetQuoteRefreshFailureCount() } just runs

        val exchangeRateRepository = mockk<ExchangeRateRepository>()
        every { exchangeRateRepository.observeQuotes() } returns flowOf(
            ExchangeRate(quotes = mapOf("USD" to 475.0), fetchedAt = 1L),
        )
        coEvery { exchangeRateRepository.fetchAndStoreQuotes() } returns ExchangeRate(
            quotes = mapOf("USD" to 480.0),
            fetchedAt = 2L,
        )

        val viewModel = createViewModel(
            userPreferences = userPreferences,
            exchangeRateRepository = exchangeRateRepository,
        )
        advanceUntilIdle()

        viewModel.refreshExchangeRate()
        advanceUntilIdle()

        coVerify(exactly = 1) { userPreferences.resetQuoteRefreshFailureCount() }
        coVerify(exactly = 0) { userPreferences.incrementQuoteRefreshFailureCount() }
    }

    @Test
    fun `failed refresh increments failure counter`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val userPreferences = mockUserPreferences()
        coEvery { userPreferences.incrementQuoteRefreshFailureCount() } returns 1

        val exchangeRateRepository = mockk<ExchangeRateRepository>()
        every { exchangeRateRepository.observeQuotes() } returns flowOf(null)
        coEvery { exchangeRateRepository.fetchAndStoreQuotes() } throws IOException("timeout")

        val viewModel = createViewModel(
            userPreferences = userPreferences,
            exchangeRateRepository = exchangeRateRepository,
        )
        advanceUntilIdle()

        viewModel.refreshExchangeRate()
        advanceUntilIdle()

        coVerify(exactly = 1) { userPreferences.incrementQuoteRefreshFailureCount() }
        coVerify(exactly = 0) { userPreferences.resetQuoteRefreshFailureCount() }
        assertNotNull(viewModel.state.value.rateErrorMessage)
    }

    @Test
    fun `three failures trigger auto-switch to top currencies`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val userPreferences = mockUserPreferences()
        coEvery { userPreferences.incrementQuoteRefreshFailureCount() } returns 3
        coEvery { userPreferences.resetQuoteRefreshFailureCount() } just runs
        coEvery { userPreferences.setBaseCurrency(any()) } just runs
        coEvery { userPreferences.setTargetCurrency(any()) } just runs

        val exchangeRateRepository = mockk<ExchangeRateRepository>()
        every { exchangeRateRepository.observeQuotes() } returns flowOf(null)
        coEvery { exchangeRateRepository.fetchAndStoreQuotes() } throws IOException("timeout")

        val transactionRepository = mockk<TransactionRepository>()
        coEvery { transactionRepository.getTopCurrenciesByUsage() } returns listOf("EUR", "KZT")

        val viewModel = createViewModel(
            userPreferences = userPreferences,
            exchangeRateRepository = exchangeRateRepository,
            transactionRepository = transactionRepository,
        )
        advanceUntilIdle()

        viewModel.refreshExchangeRate()
        advanceUntilIdle()

        coVerify { userPreferences.setBaseCurrency("EUR") }
        coVerify { userPreferences.setTargetCurrency("KZT") }
        coVerify { userPreferences.resetQuoteRefreshFailureCount() }
    }

    @Test
    fun `auto-switch uses USD and EUR defaults when no transactions`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val userPreferences = mockUserPreferences()
        coEvery { userPreferences.incrementQuoteRefreshFailureCount() } returns 3
        coEvery { userPreferences.resetQuoteRefreshFailureCount() } just runs
        coEvery { userPreferences.setBaseCurrency(any()) } just runs
        coEvery { userPreferences.setTargetCurrency(any()) } just runs

        val exchangeRateRepository = mockk<ExchangeRateRepository>()
        every { exchangeRateRepository.observeQuotes() } returns flowOf(null)
        coEvery { exchangeRateRepository.fetchAndStoreQuotes() } throws IOException("timeout")

        val transactionRepository = mockk<TransactionRepository>()
        coEvery { transactionRepository.getTopCurrenciesByUsage() } returns emptyList()

        val viewModel = createViewModel(
            userPreferences = userPreferences,
            exchangeRateRepository = exchangeRateRepository,
            transactionRepository = transactionRepository,
        )
        advanceUntilIdle()

        viewModel.refreshExchangeRate()
        advanceUntilIdle()

        coVerify { userPreferences.setBaseCurrency("USD") }
        coVerify { userPreferences.setTargetCurrency("EUR") }
    }

    @Test
    fun `auto-switch uses USD default for base when only one currency in history`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val userPreferences = mockUserPreferences()
        coEvery { userPreferences.incrementQuoteRefreshFailureCount() } returns 3
        coEvery { userPreferences.resetQuoteRefreshFailureCount() } just runs
        coEvery { userPreferences.setBaseCurrency(any()) } just runs
        coEvery { userPreferences.setTargetCurrency(any()) } just runs

        val exchangeRateRepository = mockk<ExchangeRateRepository>()
        every { exchangeRateRepository.observeQuotes() } returns flowOf(null)
        coEvery { exchangeRateRepository.fetchAndStoreQuotes() } throws IOException("timeout")

        val transactionRepository = mockk<TransactionRepository>()
        // Only one currency in history — target should fall back to EUR
        coEvery { transactionRepository.getTopCurrenciesByUsage() } returns listOf("KZT")

        val viewModel = createViewModel(
            userPreferences = userPreferences,
            exchangeRateRepository = exchangeRateRepository,
            transactionRepository = transactionRepository,
        )
        advanceUntilIdle()

        viewModel.refreshExchangeRate()
        advanceUntilIdle()

        coVerify { userPreferences.setBaseCurrency("KZT") }
        coVerify { userPreferences.setTargetCurrency("EUR") }
    }

    @Test
    fun `two failures do not trigger auto-switch`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val userPreferences = mockUserPreferences()
        coEvery { userPreferences.incrementQuoteRefreshFailureCount() } returns 2

        val exchangeRateRepository = mockk<ExchangeRateRepository>()
        every { exchangeRateRepository.observeQuotes() } returns flowOf(null)
        coEvery { exchangeRateRepository.fetchAndStoreQuotes() } throws IOException("timeout")

        val viewModel = createViewModel(
            userPreferences = userPreferences,
            exchangeRateRepository = exchangeRateRepository,
        )
        advanceUntilIdle()

        viewModel.refreshExchangeRate()
        advanceUntilIdle()

        coVerify(exactly = 0) { userPreferences.setBaseCurrency(any()) }
        coVerify(exactly = 0) { userPreferences.setTargetCurrency(any()) }
        coVerify(exactly = 0) { userPreferences.resetQuoteRefreshFailureCount() }
    }

    private fun mockUserPreferences(): UserPreferences {
        val prefs = mockk<UserPreferences>()
        every { prefs.themeMode } returns flowOf("system")
        every { prefs.baseCurrency } returns flowOf("KZT")
        every { prefs.targetCurrency } returns flowOf("USD")
        return prefs
    }

    private fun createViewModel(
        userPreferences: UserPreferences = mockUserPreferences(),
        exchangeRateRepository: ExchangeRateRepository = mockk<ExchangeRateRepository>().also {
            every { it.observeQuotes() } returns flowOf(null)
        },
        transactionRepository: TransactionRepository = mockk<TransactionRepository>().also {
            coEvery { it.getTopCurrenciesByUsage() } returns emptyList()
        },
    ): SettingsViewModel {
        val application = mockk<Application>(relaxed = true)

        return SettingsViewModel(
            userPreferences = userPreferences,
            observeExchangeRateUseCase = ObserveExchangeRateUseCase(exchangeRateRepository),
            exchangeRateRepository = exchangeRateRepository,
            transactionRepository = transactionRepository,
            application = application,
        )
    }
}
