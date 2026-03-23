package com.atelbay.money_manager.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

data class StoredExchangeRate(
    val usdToKzt: Double,
    val fetchedAt: Long,
    val source: String?,
    /** Full multi-currency quotes (code → KZT per 1 unit). Null when read from legacy storage. */
    val quotes: Map<String, Double>? = null,
)

@Singleton
class UserPreferences @Inject constructor(
    private val context: Context,
) {

    val isOnboardingCompleted: Flow<Boolean> =
        context.dataStore.data.map { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] ?: false
        }

    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = true
        }
    }

    val selectedAccountId: Flow<Long?> =
        context.dataStore.data.map { prefs ->
            prefs[KEY_SELECTED_ACCOUNT_ID]
        }

    suspend fun setSelectedAccountId(id: Long?) {
        context.dataStore.edit { prefs ->
            if (id != null) {
                prefs[KEY_SELECTED_ACCOUNT_ID] = id
            } else {
                prefs.remove(KEY_SELECTED_ACCOUNT_ID)
            }
        }
    }

    val themeMode: Flow<String> =
        context.dataStore.data.map { prefs ->
            prefs[KEY_THEME_MODE] ?: "system"
        }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode
        }
    }

    val languageCode: Flow<String> =
        context.dataStore.data.map { prefs ->
            prefs[KEY_LANGUAGE_CODE] ?: "ru"
        }

    suspend fun setLanguageCode(code: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LANGUAGE_CODE] = code
        }
    }

    val baseCurrency: Flow<String> =
        context.dataStore.data.map { prefs ->
            prefs[KEY_BASE_CURRENCY] ?: "KZT"
        }

    suspend fun setBaseCurrency(currencyCode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BASE_CURRENCY] = currencyCode
        }
    }

    val targetCurrency: Flow<String> =
        context.dataStore.data.map { prefs ->
            prefs[KEY_TARGET_CURRENCY] ?: "USD"
        }

    suspend fun setTargetCurrency(currencyCode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TARGET_CURRENCY] = currencyCode
        }
    }

    val quoteRefreshFailureCount: Flow<Int> =
        context.dataStore.data.map { prefs ->
            prefs[KEY_QUOTE_REFRESH_FAILURE_COUNT] ?: 0
        }

    /**
     * Increments the consecutive quote-refresh failure counter.
     * @return the new failure count after incrementing.
     */
    suspend fun incrementQuoteRefreshFailureCount(): Int {
        var newCount = 0
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_QUOTE_REFRESH_FAILURE_COUNT] ?: 0
            newCount = current + 1
            prefs[KEY_QUOTE_REFRESH_FAILURE_COUNT] = newCount
        }
        return newCount
    }

    suspend fun resetQuoteRefreshFailureCount() {
        context.dataStore.edit { prefs ->
            prefs[KEY_QUOTE_REFRESH_FAILURE_COUNT] = 0
        }
    }

    val exchangeRate: Flow<StoredExchangeRate?> =
        context.dataStore.data.map { prefs ->
            prefs.toStoredExchangeRate()
        }

    suspend fun getExchangeRate(): StoredExchangeRate? =
        context.dataStore.data.first().toStoredExchangeRate()

    suspend fun setExchangeRate(
        usdToKzt: Double,
        fetchedAt: Long,
        source: String? = null,
    ) {
        setExchangeRate(
            quotes = mapOf("KZT" to 1.0, "USD" to usdToKzt),
            fetchedAt = fetchedAt,
            source = source,
        )
    }

    suspend fun setExchangeRate(
        quotes: Map<String, Double>,
        fetchedAt: Long,
        source: String? = null,
    ) {
        val normalizedQuotes = if (quotes.containsKey("KZT")) quotes else quotes + ("KZT" to 1.0)
        context.dataStore.edit { prefs ->
            // Persist full quotes map as JSON
            prefs[KEY_EXCHANGE_QUOTES_JSON] = quotesToJson(normalizedQuotes)
            prefs[KEY_USD_KZT_RATE_FETCHED_AT] = fetchedAt

            // Legacy backward-compat: keep writing USD scalar for older code paths
            val usdRate = normalizedQuotes["USD"]
            if (usdRate != null) {
                prefs[KEY_USD_KZT_RATE] = usdRate
            }

            if (source != null) {
                prefs[KEY_USD_KZT_RATE_SOURCE] = source
            } else {
                prefs.remove(KEY_USD_KZT_RATE_SOURCE)
            }
        }
    }

    val cachedAiParserConfigs: Flow<String?> =
        context.dataStore.data.map { prefs ->
            prefs[KEY_AI_PARSER_CONFIGS]
        }

    suspend fun setCachedAiParserConfigs(json: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AI_PARSER_CONFIGS] = json
        }
    }

    suspend fun clearCachedAiParserConfigs() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_AI_PARSER_CONFIGS)
        }
    }

    val cachedAiTableParserConfigs: Flow<String?> =
        context.dataStore.data.map { prefs ->
            prefs[KEY_AI_TABLE_PARSER_CONFIGS]
        }

    suspend fun setCachedAiTableParserConfigs(json: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AI_TABLE_PARSER_CONFIGS] = json
        }
    }

    suspend fun clearCachedAiTableParserConfigs() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_AI_TABLE_PARSER_CONFIGS)
        }
    }

    private fun androidx.datastore.preferences.core.Preferences.toStoredExchangeRate(): StoredExchangeRate? {
        val fetchedAt = this[KEY_USD_KZT_RATE_FETCHED_AT] ?: return null

        // Prefer full quotes JSON; fall back to legacy single-value USD key
        val quotesJson = this[KEY_EXCHANGE_QUOTES_JSON]
        val quotes = quotesJson?.let { jsonToQuotes(it) }
        val usdToKzt = quotes?.get("USD")
            ?: this[KEY_USD_KZT_RATE]
            ?: return null

        return StoredExchangeRate(
            usdToKzt = usdToKzt,
            fetchedAt = fetchedAt,
            source = this[KEY_USD_KZT_RATE_SOURCE],
            quotes = quotes,
        )
    }

    private companion object {
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_SELECTED_ACCOUNT_ID = longPreferencesKey("selected_account_id")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_LANGUAGE_CODE = stringPreferencesKey("language_code")
        val KEY_BASE_CURRENCY = stringPreferencesKey("base_currency")
        val KEY_TARGET_CURRENCY = stringPreferencesKey("target_currency")
        val KEY_USD_KZT_RATE = doublePreferencesKey("usd_kzt_rate")
        val KEY_USD_KZT_RATE_FETCHED_AT = longPreferencesKey("usd_kzt_rate_fetched_at")
        val KEY_USD_KZT_RATE_SOURCE = stringPreferencesKey("usd_kzt_rate_source")
        val KEY_EXCHANGE_QUOTES_JSON = stringPreferencesKey("exchange_quotes_json")
        val KEY_QUOTE_REFRESH_FAILURE_COUNT = intPreferencesKey("quote_refresh_failure_count")
        val KEY_AI_PARSER_CONFIGS = stringPreferencesKey("ai_parser_configs")
        val KEY_AI_TABLE_PARSER_CONFIGS = stringPreferencesKey("ai_table_parser_configs")

        /**
         * Simple JSON serializer for quotes map. Avoids adding org.json / Gson dependency.
         * Format: {"USD":475.0,"EUR":520.0}
         */
        fun quotesToJson(quotes: Map<String, Double>): String =
            quotes.entries.joinToString(",", "{", "}") { (code, rate) ->
                "\"$code\":$rate"
            }

        fun jsonToQuotes(json: String): Map<String, Double>? {
            if (json.isBlank()) return null
            return try {
                val content = json.trim().removeSurrounding("{", "}")
                if (content.isBlank()) return emptyMap()
                content.split(",").associate { entry ->
                    val (key, value) = entry.split(":")
                    key.trim().removeSurrounding("\"") to value.trim().toDouble()
                }
            } catch (_: Exception) {
                null
            }
        }
    }
}
