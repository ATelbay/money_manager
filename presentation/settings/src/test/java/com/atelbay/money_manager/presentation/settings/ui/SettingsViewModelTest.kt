package com.atelbay.money_manager.presentation.settings.ui

import android.app.Application
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.atelbay.money_manager.core.datastore.UserPreferences
import com.atelbay.money_manager.domain.exchangerate.model.CurrencyRate
import com.atelbay.money_manager.domain.exchangerate.model.ExchangeRateSnapshot
import com.atelbay.money_manager.domain.exchangerate.repository.ExchangeRateRepository
import com.atelbay.money_manager.domain.exchangerate.usecase.ConvertAmountUseCase
import com.atelbay.money_manager.domain.exchangerate.usecase.GetExchangeRateSnapshotUseCase
import io.mockk.every
import io.mockk.mockk
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
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `builds cross currency rate display from snapshot`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val viewModel = createViewModel(
            baseCurrency = flowOf("EUR"),
            targetCurrency = flowOf("USD"),
            snapshot = MutableStateFlow(snapshot()),
        )

        advanceUntilIdle()

        assertEquals("EUR", viewModel.state.value.baseCurrency.code)
        assertEquals("USD", viewModel.state.value.targetCurrency.code)
        assertEquals("1 EUR = 1.10 USD", viewModel.state.value.rateDisplay)
    }

    @Test
    fun `same currency pair renders no op rate display`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val viewModel = createViewModel(
            baseCurrency = flowOf("USD"),
            targetCurrency = flowOf("USD"),
            snapshot = MutableStateFlow(null),
        )

        advanceUntilIdle()

        assertEquals("1 USD = 1.00 USD", viewModel.state.value.rateDisplay)
    }

    @Test
    fun `invalid stored codes fall back to defaults`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val viewModel = createViewModel(
            baseCurrency = flowOf("XXX"),
            targetCurrency = flowOf("YYY"),
            snapshot = MutableStateFlow(snapshot()),
        )

        advanceUntilIdle()

        assertEquals("KZT", viewModel.state.value.baseCurrency.code)
        assertEquals("USD", viewModel.state.value.targetCurrency.code)
        assertEquals("1 KZT = 0.00 USD", viewModel.state.value.rateDisplay)
    }

    private fun createViewModel(
        baseCurrency: Flow<String>,
        targetCurrency: Flow<String>,
        snapshot: MutableStateFlow<ExchangeRateSnapshot?>,
    ): SettingsViewModel {
        val userPreferences = mockk<UserPreferences>()
        every { userPreferences.themeMode } returns flowOf("system")
        every { userPreferences.baseCurrency } returns baseCurrency
        every { userPreferences.targetCurrency } returns targetCurrency

        val packageManager = mockk<PackageManager>()
        val packageInfo = PackageInfo().apply {
            versionName = "1.0.0"
        }
        every { packageManager.getPackageInfo(any<String>(), 0) } returns packageInfo

        val application = mockk<Application>()
        every { application.packageManager } returns packageManager
        every { application.packageName } returns "com.atelbay.money_manager"

        return SettingsViewModel(
            userPreferences = userPreferences,
            getExchangeRateSnapshotUseCase = GetExchangeRateSnapshotUseCase(
                FakeExchangeRateRepository(snapshot),
            ),
            exchangeRateRepository = FakeExchangeRateRepository(snapshot),
            convertAmountUseCase = ConvertAmountUseCase(),
            application = application,
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

    private class FakeExchangeRateRepository(
        private val snapshot: MutableStateFlow<ExchangeRateSnapshot?>,
    ) : ExchangeRateRepository {
        override fun observeRates(): Flow<ExchangeRateSnapshot?> = snapshot

        override suspend fun saveRates(snapshot: ExchangeRateSnapshot) = Unit

        override suspend fun fetchAndStoreRates(): ExchangeRateSnapshot = snapshot.value
            ?: error("Snapshot missing")
    }
}
