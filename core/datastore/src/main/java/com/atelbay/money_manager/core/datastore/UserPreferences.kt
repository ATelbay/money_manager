package com.atelbay.money_manager.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

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

    private companion object {
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_SELECTED_ACCOUNT_ID = longPreferencesKey("selected_account_id")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
