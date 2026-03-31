package com.atelbay.money_manager.core.remoteconfig

import android.content.Context
import com.atelbay.money_manager.core.database.dao.ParserConfigDao
import com.atelbay.money_manager.core.database.entity.ParserConfigEntity
import com.atelbay.money_manager.core.datastore.UserPreferences
import com.atelbay.money_manager.core.firestore.datasource.FirestoreDataSource
import com.atelbay.money_manager.core.model.TableParserConfig
import com.atelbay.money_manager.core.model.TableParserConfigList
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParserConfigSyncer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val parserConfigDao: ParserConfigDao,
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
            val existing = parserConfigDao.getAllActive()
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
            json.decodeFromString<ParserConfigList>(defaultJson).banks
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse bundled parser configs")
            return
        }

        val entities = configs.map { config ->
            ParserConfigEntity(
                id = "seed_${config.bankId}",
                bankId = config.bankId,
                configType = "regex",
                configJson = json.encodeToString(ParserConfig.serializer(), config),
                version = 0,
                status = "active",
                source = "seed",
                updatedAt = System.currentTimeMillis(),
            )
        }

        parserConfigDao.upsertAll(entities)
        Timber.d("Seeded %d parser configs from bundled defaults", entities.size)
    }

    private suspend fun migrateDataStoreAiCaches() {
        val entities = mutableListOf<ParserConfigEntity>()
        val now = System.currentTimeMillis()

        // Migrate cached AI regex configs
        val regexJson = userPreferences.cachedAiParserConfigs.firstOrNull()
        if (!regexJson.isNullOrBlank()) {
            try {
                val configs = json.decodeFromString<ParserConfigList>(regexJson).banks
                configs.forEach { config ->
                    entities.add(
                        ParserConfigEntity(
                            id = "ai_regex_${config.bankId}_$now",
                            bankId = config.bankId,
                            configType = "regex",
                            configJson = json.encodeToString(ParserConfig.serializer(), config),
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
                val configs = json.decodeFromString<TableParserConfigList>(tableJson).configs
                configs.forEach { config ->
                    entities.add(
                        ParserConfigEntity(
                            id = "ai_table_${config.bankId}_$now",
                            bankId = config.bankId,
                            configType = "table",
                            configJson = json.encodeToString(TableParserConfig.serializer(), config),
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
            parserConfigDao.upsertAll(entities)
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
            ParserConfigEntity(
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

        parserConfigDao.upsertAll(entities)
        parserConfigDao.deleteStaleExceptLocal(entities.map { it.id })

        val remoteVersion = firestoreDataSource.getParserConfigsVersion()
        if (remoteVersion != null) {
            userPreferences.setParserConfigsGlobalVersion(remoteVersion)
        }
        Timber.d("Synced %d parser configs from Firestore", entities.size)
    }
}
