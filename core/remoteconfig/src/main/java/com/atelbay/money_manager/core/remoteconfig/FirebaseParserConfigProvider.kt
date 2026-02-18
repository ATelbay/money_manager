package com.atelbay.money_manager.core.remoteconfig

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val CONFIG_KEY = "parser_configs"
private const val FETCH_INTERVAL_SECONDS = 3600L

@Singleton
class FirebaseParserConfigProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ParserConfigProvider {

    private val json = Json { ignoreUnknownKeys = true }

    private val remoteConfig by lazy {
        Firebase.remoteConfig.apply {
            setConfigSettingsAsync(
                remoteConfigSettings {
                    minimumFetchIntervalInSeconds = FETCH_INTERVAL_SECONDS
                },
            )
            setDefaultsAsync(mapOf(CONFIG_KEY to loadDefaultJson()))
        }
    }

    private var cachedConfigs: List<ParserConfig>? = null

    override suspend fun getConfigs(): List<ParserConfig> {
        cachedConfigs?.let { return it }

        val fetched = fetchFromFirebase()
        val configJson = fetched ?: loadDefaultJson()

        return parseConfigs(configJson).also { cachedConfigs = it }
    }

    override suspend fun getConfigForBank(bankId: String): ParserConfig? {
        return getConfigs().find { it.bankId == bankId }
    }

    private suspend fun fetchFromFirebase(): String? = suspendCoroutine { cont ->
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val value = remoteConfig.getString(CONFIG_KEY)
                Timber.d("Remote Config fetched: %d chars", value.length)
                cont.resume(value.ifEmpty { null })
            } else {
                Timber.w(task.exception, "Remote Config fetch failed, using defaults")
                cont.resume(null)
            }
        }
    }

    private fun loadDefaultJson(): String {
        return context.resources
            .openRawResource(R.raw.default_parser_config)
            .bufferedReader()
            .use { it.readText() }
    }

    private fun parseConfigs(jsonString: String): List<ParserConfig> {
        return try {
            json.decodeFromString<ParserConfigList>(jsonString).banks
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse parser configs")
            emptyList()
        }
    }
}
