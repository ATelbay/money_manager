package com.atelbay.money_manager.data.exchangerate.remote

import com.atelbay.money_manager.data.exchangerate.model.NbkExchangeRateRemoteModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

class NbkExchangeRateRemoteDataSource @Inject constructor() {

    suspend fun fetchQuotes(): NbkExchangeRateRemoteModel =
        withContext(Dispatchers.IO) {
            val connection = (URL(NBK_RATES_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MILLIS
                readTimeout = READ_TIMEOUT_MILLIS
                instanceFollowRedirects = true
            }

            try {
                val responseCode = connection.responseCode
                if (responseCode !in HTTP_OK..HTTP_MULTIPLE_CHOICES) {
                    throw IOException("NBK request failed with code $responseCode")
                }

                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val quotes = parseAllQuotes(body)
                if (quotes.isEmpty()) {
                    throw IOException("Failed to parse any currency quotes from NBK response")
                }

                NbkExchangeRateRemoteModel(quotes = quotes)
            } finally {
                connection.disconnect()
            }
        }

    /**
     * Parses all currency rows from the NBK HTML page.
     * Each row has: nominal + name, code / KZT, rate value.
     * Normalizes nominal-based rows (e.g., 100 JPY = X KZT) to per-1-unit KZT rates.
     */
    internal fun parseAllQuotes(html: String): Map<String, Double> {
        val quotes = mutableMapOf<String, Double>()
        for (match in CURRENCY_ROW_REGEX.findAll(html)) {
            val nominal = match.groupValues[1].toIntOrNull() ?: continue
            val code = match.groupValues[2].uppercase()
            val rawRate = match.groupValues[3]
                .replace(",", ".")
                .toDoubleOrNull() ?: continue

            if (nominal <= 0 || rawRate <= 0.0) continue

            // Normalize: NBK quotes "100 JPY = 33.57 KZT" → 1 JPY = 0.3357 KZT
            val perUnitRate = rawRate / nominal
            quotes[code] = perUnitRate
        }
        return quotes
    }

    private companion object {
        const val NBK_RATES_URL =
            "https://nationalbank.kz/en/exchangerates/ezhednevnye-oficialnye-rynochnye-kursy-valyut"
        const val CONNECT_TIMEOUT_MILLIS = 15_000
        const val READ_TIMEOUT_MILLIS = 15_000
        const val HTTP_OK = 200
        const val HTTP_MULTIPLE_CHOICES = 299

        /**
         * Matches each currency row in the NBK HTML table:
         * Group 1 = nominal (1, 10, 100)
         * Group 2 = currency code (USD, EUR, JPY, etc.)
         * Group 3 = rate value in KZT
         */
        val CURRENCY_ROW_REGEX = Regex(
            pattern = """<td[^>]*>\s*(\d+)\s+[^<]+</td>\s*<td[^>]*>\s*([A-Z]{3})\s*/\s*KZT\s*</td>\s*<td[^>]*>\s*([0-9]+(?:[.,][0-9]+)?)\s*</td>""",
            options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
    }
}
