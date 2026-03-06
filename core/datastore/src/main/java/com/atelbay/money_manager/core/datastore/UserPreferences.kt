package com.atelbay.money_manager.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

data class StoredExchangeRateSnapshot(
    val fetchedAt: Long,
    val source: String?,
    val rates: Map<String, StoredCurrencyRate>,
)

data class StoredCurrencyRate(
    val code: String,
    val kztPerUnit: Double,
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

    val exchangeRateSnapshot: Flow<StoredExchangeRateSnapshot?> =
        context.dataStore.data.map { prefs ->
            prefs.toStoredExchangeRateSnapshot()
        }

    suspend fun getExchangeRateSnapshot(): StoredExchangeRateSnapshot? =
        context.dataStore.data.first().toStoredExchangeRateSnapshot()

    suspend fun setExchangeRateSnapshot(
        snapshot: StoredExchangeRateSnapshot,
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_EXCHANGE_RATE_SNAPSHOT] = snapshot.toJson()
        }
    }

    private fun androidx.datastore.preferences.core.Preferences.toStoredExchangeRateSnapshot(): StoredExchangeRateSnapshot? {
        this[KEY_EXCHANGE_RATE_SNAPSHOT]
            ?.let(::snapshotFromJson)
            ?.let { return it }

        val usdToKzt = this[KEY_USD_KZT_RATE] ?: return null
        val fetchedAt = this[KEY_USD_KZT_RATE_FETCHED_AT] ?: return null

        return StoredExchangeRateSnapshot(
            fetchedAt = fetchedAt,
            source = this[KEY_USD_KZT_RATE_SOURCE],
            rates = mapOf(
                KZT to StoredCurrencyRate(code = KZT, kztPerUnit = 1.0),
                USD to StoredCurrencyRate(code = USD, kztPerUnit = usdToKzt),
            ),
        )
    }

    private fun StoredExchangeRateSnapshot.toJson(): String {
        val payload = buildJsonObject {
            put("fetchedAt", JsonPrimitive(fetchedAt))
            put("source", source?.let(::JsonPrimitive) ?: JsonNull)
            put(
                "rates",
                JsonObject(
                    rates
                        .mapKeys { it.key.uppercase() }
                        .toSortedMap()
                        .mapValues { (_, value) -> JsonPrimitive(value.kztPerUnit) },
                ),
            )
        }
        return Json.encodeToString(JsonObject.serializer(), payload)
    }

    private fun snapshotFromJson(raw: String): StoredExchangeRateSnapshot? {
        return runCatching {
            val root = Json.parseToJsonElement(raw).jsonObject
            val fetchedAt = (root["fetchedAt"] as? JsonPrimitive)?.longOrNull ?: return null
            val ratesObject = root["rates"]?.jsonObject ?: return null
            val rates = ratesObject.mapNotNull { (code, element) ->
                val normalizedCode = code.trim().uppercase()
                val kztPerUnit = (element as? JsonPrimitive)?.doubleOrNull
                if (normalizedCode.isBlank() || kztPerUnit == null || kztPerUnit <= 0.0) {
                    null
                } else {
                    normalizedCode to StoredCurrencyRate(
                        code = normalizedCode,
                        kztPerUnit = kztPerUnit,
                    )
                }
            }.toMap()

            if (rates.isEmpty()) {
                null
            } else {
                StoredExchangeRateSnapshot(
                    fetchedAt = fetchedAt,
                    source = (root["source"] as? JsonPrimitive)?.contentOrNull,
                    rates = rates,
                )
            }
        }.getOrNull()
    }

    private companion object {
        const val KZT = "KZT"
        const val USD = "USD"
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_SELECTED_ACCOUNT_ID = longPreferencesKey("selected_account_id")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_BASE_CURRENCY = stringPreferencesKey("base_currency")
        val KEY_TARGET_CURRENCY = stringPreferencesKey("target_currency")
        val KEY_EXCHANGE_RATE_SNAPSHOT = stringPreferencesKey("exchange_rate_snapshot")
        val KEY_USD_KZT_RATE = doublePreferencesKey("usd_kzt_rate")
        val KEY_USD_KZT_RATE_FETCHED_AT = longPreferencesKey("usd_kzt_rate_fetched_at")
        val KEY_USD_KZT_RATE_SOURCE = stringPreferencesKey("usd_kzt_rate_source")
    }
}
