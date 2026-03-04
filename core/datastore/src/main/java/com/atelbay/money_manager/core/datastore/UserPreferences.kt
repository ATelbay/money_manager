package com.atelbay.money_manager.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
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
        context.dataStore.edit { prefs ->
            prefs[KEY_USD_KZT_RATE] = usdToKzt
            prefs[KEY_USD_KZT_RATE_FETCHED_AT] = fetchedAt

            if (source != null) {
                prefs[KEY_USD_KZT_RATE_SOURCE] = source
            } else {
                prefs.remove(KEY_USD_KZT_RATE_SOURCE)
            }
        }
    }

    private fun androidx.datastore.preferences.core.Preferences.toStoredExchangeRate(): StoredExchangeRate? {
        val usdToKzt = this[KEY_USD_KZT_RATE] ?: return null
        val fetchedAt = this[KEY_USD_KZT_RATE_FETCHED_AT] ?: return null

        return StoredExchangeRate(
            usdToKzt = usdToKzt,
            fetchedAt = fetchedAt,
            source = this[KEY_USD_KZT_RATE_SOURCE],
        )
    }

    private companion object {
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_SELECTED_ACCOUNT_ID = longPreferencesKey("selected_account_id")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_BASE_CURRENCY = stringPreferencesKey("base_currency")
        val KEY_TARGET_CURRENCY = stringPreferencesKey("target_currency")
        val KEY_USD_KZT_RATE = doublePreferencesKey("usd_kzt_rate")
        val KEY_USD_KZT_RATE_FETCHED_AT = longPreferencesKey("usd_kzt_rate_fetched_at")
        val KEY_USD_KZT_RATE_SOURCE = stringPreferencesKey("usd_kzt_rate_source")
    }
}
