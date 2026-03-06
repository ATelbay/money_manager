package com.atelbay.money_manager.data.exchangerate.remote

import com.atelbay.money_manager.data.exchangerate.model.NbkExchangeRateRemoteModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

class NbkExchangeRateRemoteDataSource @Inject constructor() {

    suspend fun fetchRates(): NbkExchangeRateRemoteModel =
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
                NbkExchangeRateRemoteModel(rates = parseRates(body))
            } finally {
                connection.disconnect()
            }
        }

    internal fun parseRates(body: String): Map<String, Double> {
        val parsedRates = RATE_ROW_REGEX.findAll(body)
            .mapNotNull { match ->
                val nominal = match.groupValues[1].toIntOrNull()
                val code = match.groupValues[2].trim().uppercase()
                val quotedValue = match.groupValues[3].replace(",", ".").toDoubleOrNull()
                if (nominal == null || nominal <= 0 || code.isBlank() || quotedValue == null || quotedValue <= 0.0) {
                    null
                } else {
                    code to (quotedValue / nominal)
                }
            }
            .toMap()

        if (parsedRates.isEmpty()) {
            throw IOException("Failed to parse exchange rates from NBK response")
        }

        return buildMap {
            put(KZT_CODE, 1.0)
            putAll(parsedRates)
        }
    }

    private companion object {
        const val NBK_RATES_URL =
            "https://nationalbank.kz/en/exchangerates/ezhednevnye-oficialnye-rynochnye-kursy-valyut"
        const val CONNECT_TIMEOUT_MILLIS = 15_000
        const val READ_TIMEOUT_MILLIS = 15_000
        const val HTTP_OK = 200
        const val HTTP_MULTIPLE_CHOICES = 299
        const val KZT_CODE = "KZT"
        val RATE_ROW_REGEX = Regex(
            pattern = """<td class="text-start">\s*([0-9]+)\s+[^<]+?\s*</td>\s*<td>\s*([A-Z]{3})\s*/\s*KZT\s*</td>\s*<td>\s*([0-9]+(?:[.,][0-9]+)?)\s*</td>""",
            options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
    }
}
