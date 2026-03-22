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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val CONFIG_KEY = "parser_configs"
private const val AI_FULL_PARSE_KEY = "ai_full_parse_enabled"
private const val GEMINI_MODEL_KEY = "gemini_model_name"
private const val DEFAULT_GEMINI_MODEL = "gemini-2.5-flash"
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
            setDefaultsAsync(
                mapOf(
                    CONFIG_KEY to loadDefaultJson(),
                    AI_FULL_PARSE_KEY to false,
                    GEMINI_MODEL_KEY to DEFAULT_GEMINI_MODEL,
                ),
            )
        }
    }

    private var cachedConfigs: List<ParserConfig>? = null
    private val mutex = Mutex()

    override suspend fun getConfigs(): List<ParserConfig> {
        cachedConfigs?.let { return it }

        return mutex.withLock {
            cachedConfigs?.let { return@withLock it }

            val fetched = fetchFromFirebase()
            val configJson = fetched ?: loadDefaultJson()

            parseConfigs(configJson).also { cachedConfigs = it }
        }
    }

    override suspend fun getConfigForBank(bankId: String): ParserConfig? {
        return getConfigs().find { it.bankId == bankId }
    }

    private suspend fun fetchFromFirebase(): String? = suspendCancellableCoroutine { cont ->
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

    override fun isAiFullParseEnabled(): Boolean {
        return remoteConfig.getBoolean(AI_FULL_PARSE_KEY)
    }

    override fun getGeminiModelName(): String {
        return remoteConfig.getString(GEMINI_MODEL_KEY).ifEmpty { DEFAULT_GEMINI_MODEL }
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
