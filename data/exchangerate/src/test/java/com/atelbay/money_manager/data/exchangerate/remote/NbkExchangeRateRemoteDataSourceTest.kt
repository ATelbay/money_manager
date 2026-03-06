package com.atelbay.money_manager.data.exchangerate.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class NbkExchangeRateRemoteDataSourceTest {

    private val dataSource = NbkExchangeRateRemoteDataSource()

    @Test
    fun `parseRates normalizes nominal based rows into kzt per unit`() {
        val html = """
            <table>
              <tr>
                <td class="text-start">1 US DOLLAR</td>
                <td>USD / KZT</td>
                <td>493.36</td>
              </tr>
              <tr>
                <td class="text-start">100 JAPANESE YEN</td>
                <td>JPY / KZT</td>
                <td>314.00</td>
              </tr>
              <tr>
                <td class="text-start">1 EURO</td>
                <td>EUR / KZT</td>
                <td>572.84</td>
              </tr>
            </table>
        """.trimIndent()

        val result = dataSource.parseRates(html)

        assertEquals(1.0, result.getValue("KZT"), 0.0)
        assertEquals(493.36, result.getValue("USD"), 0.0)
        assertEquals(3.14, result.getValue("JPY"), 0.0)
        assertEquals(572.84, result.getValue("EUR"), 0.0)
    }
}
