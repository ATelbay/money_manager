package com.atelbay.money_manager.data.exchangerate.remote

import com.atelbay.money_manager.data.exchangerate.model.NbkExchangeRateRemoteModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

class NbkExchangeRateRemoteDataSource @Inject constructor() {

    suspend fun fetchUsdKztRate(): NbkExchangeRateRemoteModel =
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
                val usdToKzt = USD_RATE_REGEX.find(body)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.replace(",", ".")
                    ?.toDoubleOrNull()
                    ?: throw IOException("Failed to parse USD/KZT rate from NBK response")

                if (usdToKzt <= 0.0) {
                    throw IOException("Parsed USD/KZT rate is invalid: $usdToKzt")
                }

                NbkExchangeRateRemoteModel(usdToKzt = usdToKzt)
            } finally {
                connection.disconnect()
            }
        }

    private companion object {
        const val NBK_RATES_URL =
            "https://nationalbank.kz/en/exchangerates/ezhednevnye-oficialnye-rynochnye-kursy-valyut"
        const val CONNECT_TIMEOUT_MILLIS = 15_000
        const val READ_TIMEOUT_MILLIS = 15_000
        const val HTTP_OK = 200
        const val HTTP_MULTIPLE_CHOICES = 299
        val USD_RATE_REGEX = Regex(
            pattern = """<td class="text-start">\s*1 US DOLLAR\s*</td>\s*<td>\s*USD / KZT\s*</td>\s*<td>\s*([0-9]+(?:[.,][0-9]+)?)\s*</td>""",
            options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
    }
}
