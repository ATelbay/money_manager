package com.atelbay.money_manager.data.exchangerate.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NbkExchangeRateRemoteDataSourceTest {

    private val dataSource = NbkExchangeRateRemoteDataSource()

    @Test
    fun `parseAllQuotes extracts multiple currency rows`() {
        val html = buildNbkHtml(
            currencyRow(nominal = 1, code = "USD", rate = "475.00"),
            currencyRow(nominal = 1, code = "EUR", rate = "520.50"),
            currencyRow(nominal = 1, code = "GBP", rate = "600.00"),
        )

        val quotes = dataSource.parseAllQuotes(html)

        assertEquals(3, quotes.size)
        assertEquals(475.0, quotes["USD"]!!, 0.0)
        assertEquals(520.5, quotes["EUR"]!!, 0.0)
        assertEquals(600.0, quotes["GBP"]!!, 0.0)
    }

    @Test
    fun `parseAllQuotes normalizes nominal-based rates to per-unit`() {
        // 100 JPY = 33.57 KZT → 1 JPY = 0.3357 KZT
        // 10 KRW = 3.42 KZT → 1 KRW = 0.342 KZT
        val html = buildNbkHtml(
            currencyRow(nominal = 1, code = "USD", rate = "475.00"),
            currencyRow(nominal = 100, code = "JPY", rate = "33.57"),
            currencyRow(nominal = 10, code = "KRW", rate = "3.42"),
        )

        val quotes = dataSource.parseAllQuotes(html)

        assertEquals(3, quotes.size)
        assertEquals(475.0, quotes["USD"]!!, 0.0)
        assertEquals(0.3357, quotes["JPY"]!!, 0.0001)
        assertEquals(0.342, quotes["KRW"]!!, 0.0001)
    }

    @Test
    fun `parseAllQuotes handles comma decimal separator`() {
        val html = buildNbkHtml(
            currencyRow(nominal = 1, code = "EUR", rate = "520,50"),
        )

        val quotes = dataSource.parseAllQuotes(html)

        assertEquals(520.5, quotes["EUR"]!!, 0.0)
    }

    @Test
    fun `parseAllQuotes returns empty map for invalid html`() {
        val quotes = dataSource.parseAllQuotes("<html><body>No table</body></html>")

        assertTrue(quotes.isEmpty())
    }

    @Test
    fun `parseAllQuotes skips rows with zero nominal or rate`() {
        val html = buildNbkHtml(
            currencyRow(nominal = 0, code = "BAD", rate = "100.00"),
            currencyRow(nominal = 1, code = "USD", rate = "475.00"),
        )

        val quotes = dataSource.parseAllQuotes(html)

        assertEquals(1, quotes.size)
        assertEquals(475.0, quotes["USD"]!!, 0.0)
    }

    @Test
    fun `parseAllQuotes normalizes currency code to uppercase`() {
        // The regex uses IGNORE_CASE, but the code calls .uppercase()
        val html = buildNbkHtml(
            currencyRow(nominal = 1, code = "usd", rate = "475.00"),
        )

        val quotes = dataSource.parseAllQuotes(html)

        assertTrue(quotes.containsKey("USD"))
    }

    private fun currencyRow(nominal: Int, code: String, rate: String): String =
        """
        <td>$nominal ${code}Currency</td>
        <td>$code / KZT</td>
        <td>$rate</td>
        """.trimIndent()

    private fun buildNbkHtml(vararg rows: String): String {
        val tableRows = rows.joinToString("\n") { "<tr>$it</tr>" }
        return "<html><body><table>$tableRows</table></body></html>"
    }
}
