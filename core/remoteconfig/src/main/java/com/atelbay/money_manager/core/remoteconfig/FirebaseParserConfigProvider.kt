package com.atelbay.money_manager.core.remoteconfig

import com.atelbay.money_manager.core.database.dao.ParserConfigDao
import com.atelbay.money_manager.core.model.TableParserConfig
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val AI_FULL_PARSE_KEY = "ai_full_parse_enabled"
private const val GEMINI_MODEL_KEY = "gemini_model_name"
private const val DEFAULT_GEMINI_MODEL = "gemini-3-flash-preview"
private const val FETCH_INTERVAL_SECONDS = 3600L

@Singleton
class FirebaseParserConfigProvider @Inject constructor(
    private val syncer: ParserConfigSyncer,
    private val parserConfigDao: ParserConfigDao,
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
                    AI_FULL_PARSE_KEY to false,
                    GEMINI_MODEL_KEY to DEFAULT_GEMINI_MODEL,
                ),
            )
        }
    }

    override suspend fun getConfigs(): List<ParserConfig> {
        syncer.ensureInitialized()
        return parserConfigDao.getActiveByType("regex").map {
            json.decodeFromString<ParserConfig>(it.configJson)
        }
    }

    override suspend fun getTableConfigs(): List<TableParserConfig> {
        syncer.ensureInitialized()
        return parserConfigDao.getActiveByType("table").map {
            json.decodeFromString<TableParserConfig>(it.configJson)
        }
    }

    override suspend fun getConfigForBank(bankId: String): ParserConfig? {
        syncer.ensureInitialized()
        return parserConfigDao.getByBankIdAndType(bankId, "regex")?.let {
            json.decodeFromString<ParserConfig>(it.configJson)
        }
    }

    override fun isAiFullParseEnabled(): Boolean {
        return remoteConfig.getBoolean(AI_FULL_PARSE_KEY)
    }

    override fun getGeminiModelName(): String {
        return remoteConfig.getString(GEMINI_MODEL_KEY).ifEmpty { DEFAULT_GEMINI_MODEL }
    }
}
