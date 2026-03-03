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
import org.junit.Test
import java.io.IOException
import kotlin.test.assertFailsWith

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
    fun `fetchAndStoreRate returns fresh remote rate and caches it`() = runTest {
        coEvery { remoteDataSource.fetchUsdKztRate() } returns NbkExchangeRateRemoteModel(usdToKzt = 475.0)
        coEvery {
            userPreferences.setExchangeRate(
                usdToKzt = any(),
                fetchedAt = any(),
                source = any(),
            )
        } just runs

        val result = repository.fetchAndStoreRate()

        assertEquals(475.0, result.usdToKzt, 0.0)
        assertTrue(result.fetchedAt > 0L)
        coVerify(exactly = 1) {
            userPreferences.setExchangeRate(
                usdToKzt = 475.0,
                fetchedAt = result.fetchedAt,
                source = "NBK",
            )
        }
    }

    @Test
    fun `fetchAndStoreRate falls back to cached rate when remote fetch fails`() = runTest {
        val networkError = IOException("network down")
        coEvery { remoteDataSource.fetchUsdKztRate() } throws networkError
        coEvery { userPreferences.getExchangeRate() } returns StoredExchangeRate(
            usdToKzt = 470.0,
            fetchedAt = 123L,
            source = "NBK",
        )

        val result = repository.fetchAndStoreRate()

        assertEquals(470.0, result.usdToKzt, 0.0)
        assertEquals(123L, result.fetchedAt)
    }

    @Test
    fun `fetchAndStoreRate rethrows remote error when cache is empty`() = runTest {
        val networkError = IOException("network down")
        coEvery { remoteDataSource.fetchUsdKztRate() } throws networkError
        coEvery { userPreferences.getExchangeRate() } returns null

        val thrown = assertFailsWith<IOException> {
            repository.fetchAndStoreRate()
        }

        assertSame(networkError, thrown)
    }
}
