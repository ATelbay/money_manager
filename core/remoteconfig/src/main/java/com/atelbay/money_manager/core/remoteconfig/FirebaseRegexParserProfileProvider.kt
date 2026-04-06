package com.atelbay.money_manager.core.remoteconfig

import com.atelbay.money_manager.core.database.dao.RegexParserProfileDao
import com.atelbay.money_manager.core.model.TableParserProfile
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private const val AI_FULL_PARSE_KEY = "ai_full_parse_enabled"
private const val GEMINI_MODEL_KEY = "gemini_model_name"
private const val DEFAULT_GEMINI_MODEL = "gemini-3-flash-preview"
private const val FETCH_INTERVAL_SECONDS = 3600L

@Singleton
class FirebaseRegexParserProfileProvider @Inject constructor(
    private val syncer: RegexParserProfileSyncer,
    private val regexParserProfileDao: RegexParserProfileDao,
) : RegexParserProfileProvider {

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

    override suspend fun getConfigs(): List<RegexParserProfile> {
        syncer.ensureInitialized()
        return regexParserProfileDao.getActiveByType("regex").mapNotNull {
            try {
                json.decodeFromString<RegexParserProfile>(it.configJson)
            } catch (e: Exception) {
                Timber.w(e, "Skipping malformed regex config: %s", it.id)
                null
            }
        }
    }

    override suspend fun getTableConfigs(): List<TableParserProfile> {
        syncer.ensureInitialized()
        return regexParserProfileDao.getActiveByType("table").mapNotNull {
            try {
                json.decodeFromString<TableParserProfile>(it.configJson)
            } catch (e: Exception) {
                Timber.w(e, "Skipping malformed table config: %s", it.id)
                null
            }
        }
    }

    override suspend fun getConfigForBank(bankId: String): RegexParserProfile? {
        syncer.ensureInitialized()
        return regexParserProfileDao.getByBankIdAndType(bankId, "regex")?.let {
            try {
                json.decodeFromString<RegexParserProfile>(it.configJson)
            } catch (e: Exception) {
                Timber.w(e, "Skipping malformed config for bank: %s", bankId)
                null
            }
        }
    }

    override fun isAiFullParseEnabled(): Boolean {
        return remoteConfig.getBoolean(AI_FULL_PARSE_KEY)
    }

    override fun getGeminiModelName(): String {
        return remoteConfig.getString(GEMINI_MODEL_KEY).ifEmpty { DEFAULT_GEMINI_MODEL }
    }
}
