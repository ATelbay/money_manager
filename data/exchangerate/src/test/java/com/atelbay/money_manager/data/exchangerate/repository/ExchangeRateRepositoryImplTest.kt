package com.atelbay.money_manager.data.exchangerate.repository

import com.atelbay.money_manager.core.datastore.StoredCurrencyRate
import com.atelbay.money_manager.core.datastore.StoredExchangeRateSnapshot
import com.atelbay.money_manager.core.datastore.UserPreferences
import com.atelbay.money_manager.data.exchangerate.model.NbkExchangeRateRemoteModel
import com.atelbay.money_manager.data.exchangerate.remote.NbkExchangeRateRemoteDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class ExchangeRateRepositoryImplTest {

    private val remoteDataSource = mockk<NbkExchangeRateRemoteDataSource>()
    private val userPreferences = mockk<UserPreferences>()
    private val repository = ExchangeRateRepositoryImpl(
        remoteDataSource = remoteDataSource,
        userPreferences = userPreferences,
    )

    init {
        every { userPreferences.exchangeRateSnapshot } returns emptyFlow()
    }

    @Test
    fun `fetchAndStoreRates returns fresh remote snapshot and caches it`() = runTest {
        coEvery { remoteDataSource.fetchRates() } returns NbkExchangeRateRemoteModel(
            rates = mapOf(
                "KZT" to 1.0,
                "USD" to 475.0,
                "EUR" to 550.0,
            ),
        )
        coEvery { userPreferences.setExchangeRateSnapshot(any()) } just runs

        val result = repository.fetchAndStoreRates()

        assertEquals(475.0, result.rates.getValue("USD").kztPerUnit, 0.0)
        assertEquals(550.0, result.rates.getValue("EUR").kztPerUnit, 0.0)
        assertTrue(result.fetchedAt > 0L)
        coVerify(exactly = 1) {
            userPreferences.setExchangeRateSnapshot(
                withArg { stored ->
                    assertEquals(475.0, stored.rates.getValue("USD").kztPerUnit, 0.0)
                    assertEquals(550.0, stored.rates.getValue("EUR").kztPerUnit, 0.0)
                },
            )
        }
    }

    @Test
    fun `fetchAndStoreRates falls back to cached snapshot when remote fetch fails`() = runTest {
        val networkError = IOException("network down")
        coEvery { remoteDataSource.fetchRates() } throws networkError
        coEvery { userPreferences.getExchangeRateSnapshot() } returns StoredExchangeRateSnapshot(
            fetchedAt = 123L,
            source = "NBK",
            rates = mapOf(
                "KZT" to StoredCurrencyRate(code = "KZT", kztPerUnit = 1.0),
                "USD" to StoredCurrencyRate(code = "USD", kztPerUnit = 470.0),
            ),
        )

        val result = repository.fetchAndStoreRates()

        assertEquals(470.0, result.rates.getValue("USD").kztPerUnit, 0.0)
        assertEquals(123L, result.fetchedAt)
    }

    @Test
    fun `fetchAndStoreRates rethrows remote error when cache is empty`() = runTest {
        val networkError = IOException("network down")
        coEvery { remoteDataSource.fetchRates() } throws networkError
        coEvery { userPreferences.getExchangeRateSnapshot() } returns null

        val thrown = try {
            repository.fetchAndStoreRates()
            fail("Expected IOException to be thrown")
        } catch (exception: IOException) {
            exception
        }

        assertSame(networkError, thrown)
    }
}
