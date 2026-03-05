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

        assertEquals(475.0, result.quotes["USD"]!!, 0.0)
        assertEquals(520.0, result.quotes["EUR"]!!, 0.0)
        assertTrue(result.fetchedAt > 0L)
        coVerify(exactly = 1) {
            userPreferences.setExchangeRate(
                quotes = remoteQuotes,
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
}
