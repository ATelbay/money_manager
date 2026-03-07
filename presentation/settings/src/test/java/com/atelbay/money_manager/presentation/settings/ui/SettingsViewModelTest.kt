package com.atelbay.money_manager.presentation.settings.ui

import android.app.Application
import com.atelbay.money_manager.core.datastore.UserPreferences
import com.atelbay.money_manager.data.sync.LoginSyncOrchestrator
import com.atelbay.money_manager.data.sync.SyncManager
import com.atelbay.money_manager.data.sync.SyncStatus
import com.atelbay.money_manager.domain.auth.usecase.ObserveAuthUserUseCase
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

        val prefs = createPreferenceHarness()
        val exchangeRateRepository = mockExchangeRateRepository(
            observedRates = flowOf(snapshot(mapOf("KZT" to 1.0, "USD" to 475.0))),
        )
        coEvery { exchangeRateRepository.fetchAndStoreQuotes() } returns snapshot(
            mapOf("KZT" to 1.0, "USD" to 480.0),
        )

        val viewModel = createViewModel(
            preferenceHarness = prefs,
            exchangeRateRepository = exchangeRateRepository,
        )
        advanceUntilIdle()

        viewModel.refreshExchangeRate()
        advanceUntilIdle()

        coVerify(exactly = 1) { prefs.preferences.resetQuoteRefreshFailureCount() }
        coVerify(exactly = 0) { prefs.preferences.incrementQuoteRefreshFailureCount() }
    }

    @Test
    fun `failed refresh increments failure counter`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val prefs = createPreferenceHarness()
        coEvery { prefs.preferences.incrementQuoteRefreshFailureCount() } returns 1
        val exchangeRateRepository = mockExchangeRateRepository(observedRates = flowOf(null))
        coEvery { exchangeRateRepository.fetchAndStoreQuotes() } throws IOException("timeout")

        val viewModel = createViewModel(
            preferenceHarness = prefs,
            exchangeRateRepository = exchangeRateRepository,
        )
        advanceUntilIdle()

        viewModel.refreshExchangeRate()
        advanceUntilIdle()

        coVerify(exactly = 1) { prefs.preferences.incrementQuoteRefreshFailureCount() }
        coVerify(exactly = 0) { prefs.preferences.resetQuoteRefreshFailureCount() }
        assertNotNull(viewModel.state.value.rateErrorMessage)
    }

    @Test
    fun `three failures trigger auto switch to top currencies`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val prefs = createPreferenceHarness()
        coEvery { prefs.preferences.incrementQuoteRefreshFailureCount() } returns 3
        val exchangeRateRepository = mockExchangeRateRepository(observedRates = flowOf(null))
        coEvery { exchangeRateRepository.fetchAndStoreQuotes() } throws IOException("timeout")

        val transactionRepository = mockk<TransactionRepository>()
        coEvery { transactionRepository.getTopCurrenciesByUsage() } returns listOf("EUR", "KZT")

        val viewModel = createViewModel(
            preferenceHarness = prefs,
            exchangeRateRepository = exchangeRateRepository,
            transactionRepository = transactionRepository,
        )
        advanceUntilIdle()

        viewModel.refreshExchangeRate()
        advanceUntilIdle()

        assertEquals("EUR", prefs.baseCurrency.value)
        assertEquals("KZT", prefs.targetCurrency.value)
        coVerify { prefs.preferences.resetQuoteRefreshFailureCount() }
    }

    @Test
    fun `auto switch uses USD and EUR defaults when no transactions`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val prefs = createPreferenceHarness()
        coEvery { prefs.preferences.incrementQuoteRefreshFailureCount() } returns 3
        val exchangeRateRepository = mockExchangeRateRepository(observedRates = flowOf(null))
        coEvery { exchangeRateRepository.fetchAndStoreQuotes() } throws IOException("timeout")

        val transactionRepository = mockk<TransactionRepository>()
        coEvery { transactionRepository.getTopCurrenciesByUsage() } returns emptyList()

        val viewModel = createViewModel(
            preferenceHarness = prefs,
            exchangeRateRepository = exchangeRateRepository,
            transactionRepository = transactionRepository,
        )
        advanceUntilIdle()

        viewModel.refreshExchangeRate()
        advanceUntilIdle()

        assertEquals("USD", prefs.baseCurrency.value)
        assertEquals("EUR", prefs.targetCurrency.value)
    }

    @Test
    fun `invalid stored base currency sanitizes to KZT and persists`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val prefs = createPreferenceHarness(baseCurrency = "BAD")

        createViewModel(preferenceHarness = prefs)
        advanceUntilIdle()

        assertEquals("KZT", prefs.baseCurrency.value)
        coVerify { prefs.preferences.setBaseCurrency("KZT") }
    }

    @Test
    fun `invalid stored target currency sanitizes to USD and persists`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val prefs = createPreferenceHarness(targetCurrency = "BAD")

        createViewModel(preferenceHarness = prefs)
        advanceUntilIdle()

        assertEquals("USD", prefs.targetCurrency.value)
        coVerify { prefs.preferences.setTargetCurrency("USD") }
    }

    @Test
    fun `valid EUR USD pair displays derived rate`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val prefs = createPreferenceHarness(baseCurrency = "EUR", targetCurrency = "USD")
        val viewModel = createViewModel(
            preferenceHarness = prefs,
            exchangeRateRepository = mockExchangeRateRepository(
                observedRates = flowOf(
                    snapshot(
                        mapOf(
                            "KZT" to 1.0,
                            "USD" to 475.0,
                            "EUR" to 520.0,
                        ),
                    ),
                ),
            ),
        )

        advanceUntilIdle()

        assertEquals("1 EUR = 1.09 USD", viewModel.state.value.rateDisplay)
        assertEquals(true, viewModel.state.value.hasRateSnapshot)
    }

    @Test
    fun `same currency pair displays unity rate`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val prefs = createPreferenceHarness(baseCurrency = "USD", targetCurrency = "USD")
        val viewModel = createViewModel(
            preferenceHarness = prefs,
            exchangeRateRepository = mockExchangeRateRepository(
                observedRates = flowOf(snapshot(mapOf("KZT" to 1.0, "USD" to 475.0))),
            ),
        )

        advanceUntilIdle()

        assertEquals("1 USD = 1.00 USD", viewModel.state.value.rateDisplay)
    }

    @Test
    fun `missing selected pair quote shows no derived rate but keeps snapshot state`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val prefs = createPreferenceHarness(baseCurrency = "EUR", targetCurrency = "USD")
        val viewModel = createViewModel(
            preferenceHarness = prefs,
            exchangeRateRepository = mockExchangeRateRepository(
                observedRates = flowOf(snapshot(mapOf("KZT" to 1.0, "USD" to 475.0))),
            ),
        )

        advanceUntilIdle()

        assertEquals(true, viewModel.state.value.hasRateSnapshot)
        assertNull(viewModel.state.value.rateDisplay)
    }

    @Test
    fun `manual base selection does not rewrite target currency`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val prefs = createPreferenceHarness(baseCurrency = "KZT", targetCurrency = "USD")
        val viewModel = createViewModel(preferenceHarness = prefs)
        advanceUntilIdle()

        viewModel.setBaseCurrency(SupportedCurrencies.fromCode("EUR"))
        advanceUntilIdle()

        assertEquals("EUR", prefs.baseCurrency.value)
        assertEquals("USD", prefs.targetCurrency.value)
    }

    private fun createViewModel(
        preferenceHarness: PreferenceHarness = createPreferenceHarness(),
        exchangeRateRepository: ExchangeRateRepository = mockExchangeRateRepository(),
        transactionRepository: TransactionRepository = mockk<TransactionRepository>().also {
            coEvery { it.getTopCurrenciesByUsage() } returns emptyList()
        },
    ): SettingsViewModel {
        val application = mockk<Application>(relaxed = true)

        val observeAuthUser = mockk<ObserveAuthUserUseCase>().also {
            every { it() } returns flowOf(null)
        }
        val syncManager = mockk<SyncManager>().also {
            every { it.syncStatus } returns MutableStateFlow(SyncStatus.Idle)
        }
        val loginSyncOrchestrator = mockk<LoginSyncOrchestrator>(relaxed = true)

        return SettingsViewModel(
            userPreferences = preferenceHarness.preferences,
            observeExchangeRateUseCase = ObserveExchangeRateUseCase(exchangeRateRepository),
            exchangeRateRepository = exchangeRateRepository,
            transactionRepository = transactionRepository,
            observeAuthUser = observeAuthUser,
            syncManager = syncManager,
            loginSyncOrchestrator = loginSyncOrchestrator,
            application = application,
        )
    }

    private fun mockExchangeRateRepository(
        observedRates: Flow<ExchangeRate?> = flowOf(null),
    ): ExchangeRateRepository {
        val repository = mockk<ExchangeRateRepository>()
        every { repository.observeQuotes() } returns observedRates
        coEvery { repository.fetchAndStoreQuotes() } throws IOException("unused")
        return repository
    }

    private fun createPreferenceHarness(
        baseCurrency: String = "KZT",
        targetCurrency: String = "USD",
    ): PreferenceHarness {
        val preferences = mockk<UserPreferences>()
        val baseFlow = MutableStateFlow(baseCurrency)
        val targetFlow = MutableStateFlow(targetCurrency)

        every { preferences.themeMode } returns flowOf("system")
        every { preferences.languageCode } returns flowOf("ru")
        every { preferences.baseCurrency } returns baseFlow
        every { preferences.targetCurrency } returns targetFlow

        coEvery { preferences.setBaseCurrency(any()) } coAnswers {
            baseFlow.value = firstArg()
            Unit
        }
        coEvery { preferences.setTargetCurrency(any()) } coAnswers {
            targetFlow.value = firstArg()
            Unit
        }
        coEvery { preferences.resetQuoteRefreshFailureCount() } just runs
        coEvery { preferences.incrementQuoteRefreshFailureCount() } returns 1

        return PreferenceHarness(
            preferences = preferences,
            baseCurrency = baseFlow,
            targetCurrency = targetFlow,
        )
    }

    private fun snapshot(quotes: Map<String, Double>) = ExchangeRate(
        quotes = quotes,
        fetchedAt = 1L,
        source = "NBK",
    )

    private data class PreferenceHarness(
        val preferences: UserPreferences,
        val baseCurrency: MutableStateFlow<String>,
        val targetCurrency: MutableStateFlow<String>,
    )
}
