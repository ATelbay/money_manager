package com.atelbay.money_manager.data.exchangerate.repository

import com.atelbay.money_manager.core.datastore.StoredExchangeRate
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
        every { userPreferences.exchangeRate } returns emptyFlow()
    }

    @Test
    fun `fetchAndStoreQuotes returns fresh remote rate and caches it`() = runTest {
        val remoteQuotes = mapOf("USD" to 475.0, "EUR" to 520.0)
        coEvery { remoteDataSource.fetchQuotes() } returns NbkExchangeRateRemoteModel(quotes = remoteQuotes)
        coEvery {
            userPreferences.setExchangeRate(
                quotes = any(),
                fetchedAt = any(),
                source = any(),
            )
        } just runs

        val result = repository.fetchAndStoreQuotes()

        assertEquals("NBK", result.source)
        assertEquals(1.0, result.quotes["KZT"]!!, 0.0)
        assertEquals(475.0, result.quotes["USD"]!!, 0.0)
        assertEquals(520.0, result.quotes["EUR"]!!, 0.0)
        assertTrue(result.fetchedAt > 0L)
        coVerify(exactly = 1) {
            userPreferences.setExchangeRate(
                quotes = remoteQuotes + ("KZT" to 1.0),
                fetchedAt = result.fetchedAt,
                source = "NBK",
            )
        }
    }

    @Test
    fun `fetchAndStoreQuotes falls back to cached rate when remote fetch fails`() = runTest {
        val networkError = IOException("network down")
        coEvery { remoteDataSource.fetchQuotes() } throws networkError
        coEvery { userPreferences.getExchangeRate() } returns StoredExchangeRate(
            usdToKzt = 470.0,
            fetchedAt = 123L,
            source = "NBK",
        )

        val result = repository.fetchAndStoreQuotes()

        assertEquals("NBK", result.source)
        assertEquals(1.0, result.quotes["KZT"]!!, 0.0)
        assertEquals(470.0, result.quotes["USD"]!!, 0.0)
        assertEquals(123L, result.fetchedAt)
    }

    @Test
    fun `fetchAndStoreQuotes rethrows remote error when cache is empty`() = runTest {
        val networkError = IOException("network down")
        coEvery { remoteDataSource.fetchQuotes() } throws networkError
        coEvery { userPreferences.getExchangeRate() } returns null

        val thrown = try {
            repository.fetchAndStoreQuotes()
            fail("Expected IOException to be thrown")
        } catch (exception: IOException) {
            exception
        }

        assertSame(networkError, thrown)
    }

    @Test
    fun `fetchAndStoreQuotes falls back to multi-currency cached quotes on failure`() = runTest {
        val cachedQuotes = mapOf("USD" to 470.0, "EUR" to 515.0, "GBP" to 590.0)
        coEvery { remoteDataSource.fetchQuotes() } throws IOException("timeout")
        coEvery { userPreferences.getExchangeRate() } returns StoredExchangeRate(
            usdToKzt = 470.0,
            fetchedAt = 456L,
            source = "NBK",
            quotes = cachedQuotes,
        )

        val result = repository.fetchAndStoreQuotes()

        assertEquals(4, result.quotes.size)
        assertEquals(1.0, result.quotes["KZT"]!!, 0.0)
        assertEquals(470.0, result.quotes["USD"]!!, 0.0)
        assertEquals(515.0, result.quotes["EUR"]!!, 0.0)
        assertEquals(590.0, result.quotes["GBP"]!!, 0.0)
        assertEquals(456L, result.fetchedAt)
        assertEquals("NBK", result.source)
    }

    @Test
    fun `fetchAndStoreQuotes persists all remote quotes not just USD`() = runTest {
        val remoteQuotes = mapOf("USD" to 475.0, "EUR" to 520.0, "JPY" to 0.34)
        coEvery { remoteDataSource.fetchQuotes() } returns NbkExchangeRateRemoteModel(quotes = remoteQuotes)
        coEvery {
            userPreferences.setExchangeRate(
                quotes = any(),
                fetchedAt = any(),
                source = any(),
            )
        } just runs

        val result = repository.fetchAndStoreQuotes()

        assertEquals(4, result.quotes.size)
        assertEquals(1.0, result.quotes["KZT"]!!, 0.0)
        assertEquals(0.34, result.quotes["JPY"]!!, 0.0)
        coVerify {
            userPreferences.setExchangeRate(
                quotes = remoteQuotes + ("KZT" to 1.0),
                fetchedAt = any(),
                source = "NBK",
            )
        }
    }

    @Test
    fun `legacy USD cache is surfaced as minimal KZT USD snapshot`() = runTest {
        coEvery { remoteDataSource.fetchQuotes() } throws IOException("timeout")
        coEvery { userPreferences.getExchangeRate() } returns StoredExchangeRate(
            usdToKzt = 468.0,
            fetchedAt = 789L,
            source = "NBK",
            quotes = null,
        )

        val result = repository.fetchAndStoreQuotes()

        assertEquals(2, result.quotes.size)
        assertEquals(1.0, result.quotes["KZT"]!!, 0.0)
        assertEquals(468.0, result.quotes["USD"]!!, 0.0)
        assertEquals("NBK", result.source)
    }
}
