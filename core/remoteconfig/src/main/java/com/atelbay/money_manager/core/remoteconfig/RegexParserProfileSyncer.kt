package com.atelbay.money_manager.core.remoteconfig

import android.content.Context
import com.atelbay.money_manager.core.database.dao.RegexParserProfileDao
import com.atelbay.money_manager.core.database.entity.RegexParserProfileEntity
import com.atelbay.money_manager.core.datastore.UserPreferences
import com.atelbay.money_manager.core.firestore.datasource.FirestoreDataSource
import com.atelbay.money_manager.core.model.TableParserProfile
import com.atelbay.money_manager.core.model.TableParserProfileList
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RegexParserProfileSyncer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val regexParserProfileDao: RegexParserProfileDao,
    private val firestoreDataSource: FirestoreDataSource,
    private val userPreferences: UserPreferences,
) {

    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private var initialized = false

    suspend fun ensureInitialized() {
        if (initialized) return
        mutex.withLock {
            if (initialized) return@withLock
            val existing = regexParserProfileDao.getAllActive()
            if (existing.isEmpty()) {
                seedFromBundledDefaults()
                migrateDataStoreAiCaches()
            }
            try {
                syncFromFirestore()
            } catch (e: Exception) {
                Timber.w(e, "Firestore sync failed, using local configs")
            }
            initialized = true
        }
    }

    suspend fun forceSync() {
        mutex.withLock {
            pullFromFirestore()
        }
    }

    private suspend fun seedFromBundledDefaults() {
        val defaultJson = context.resources
            .openRawResource(R.raw.default_parser_config)
            .bufferedReader()
            .use { it.readText() }

        val configs = try {
            json.decodeFromString<RegexParserProfileList>(defaultJson).banks
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse bundled parser configs")
            return
        }

        val entities = configs.map { config ->
            RegexParserProfileEntity(
                id = "seed_${config.bankId}",
                bankId = config.bankId,
                configType = "regex",
                configJson = json.encodeToString(RegexParserProfile.serializer(), config),
                version = 0,
                status = "active",
                source = "seed",
                updatedAt = System.currentTimeMillis(),
            )
        }

        regexParserProfileDao.upsertAll(entities)
        Timber.d("Seeded %d parser configs from bundled defaults", entities.size)
    }

    private suspend fun migrateDataStoreAiCaches() {
        val entities = mutableListOf<RegexParserProfileEntity>()
        val now = System.currentTimeMillis()

        // Migrate cached AI regex configs
        val regexJson = userPreferences.cachedAiParserConfigs.firstOrNull()
        if (!regexJson.isNullOrBlank()) {
            try {
                val configs = json.decodeFromString<RegexParserProfileList>(regexJson).banks
                configs.forEach { config ->
                    entities.add(
                        RegexParserProfileEntity(
                            id = "ai_regex_${config.bankId}_$now",
                            bankId = config.bankId,
                            configType = "regex",
                            configJson = json.encodeToString(RegexParserProfile.serializer(), config),
                            version = 0,
                            status = "active",
                            source = "ai_cached",
                            updatedAt = now,
                        ),
                    )
                }
                userPreferences.clearCachedAiParserConfigs()
                Timber.d("Migrated %d AI regex configs from DataStore", configs.size)
            } catch (e: Exception) {
                Timber.w(e, "Failed to migrate AI regex configs from DataStore")
            }
        }

        // Migrate cached AI table configs
        val tableJson = userPreferences.cachedAiTableParserConfigs.firstOrNull()
        if (!tableJson.isNullOrBlank()) {
            try {
                val configs = json.decodeFromString<TableParserProfileList>(tableJson).configs
                configs.forEach { config ->
                    entities.add(
                        RegexParserProfileEntity(
                            id = "ai_table_${config.bankId}_$now",
                            bankId = config.bankId,
                            configType = "table",
                            configJson = json.encodeToString(TableParserProfile.serializer(), config),
                            version = 0,
                            status = "active",
                            source = "ai_cached",
                            updatedAt = now,
                        ),
                    )
                }
                userPreferences.clearCachedAiTableParserConfigs()
                Timber.d("Migrated %d AI table configs from DataStore", configs.size)
            } catch (e: Exception) {
                Timber.w(e, "Failed to migrate AI table configs from DataStore")
            }
        }

        if (entities.isNotEmpty()) {
            regexParserProfileDao.upsertAll(entities)
        }
    }

    private suspend fun syncFromFirestore() {
        val remoteVersion = firestoreDataSource.getParserConfigsVersion() ?: return
        val localVersion = userPreferences.parserConfigsGlobalVersion.firstOrNull() ?: 0L
        if (remoteVersion <= localVersion) return
        pullFromFirestore()
    }

    private suspend fun pullFromFirestore() {
        val remoteDtos = firestoreDataSource.pullActiveParserConfigs()
        if (remoteDtos.isEmpty()) return

        val entities = remoteDtos.map { dto ->
            RegexParserProfileEntity(
                id = dto.id,
                bankId = dto.bankId,
                configType = dto.configType,
                configJson = dto.configJson,
                version = dto.version,
                status = dto.status,
                source = dto.source,
                updatedAt = dto.updatedAt,
            )
        }

        regexParserProfileDao.upsertAll(entities)
        regexParserProfileDao.deleteStaleExceptLocal(entities.map { it.id })

        val remoteVersion = firestoreDataSource.getParserConfigsVersion()
        if (remoteVersion != null) {
            userPreferences.setParserConfigsGlobalVersion(remoteVersion)
        }
        Timber.d("Synced %d parser configs from Firestore", entities.size)
    }
}
