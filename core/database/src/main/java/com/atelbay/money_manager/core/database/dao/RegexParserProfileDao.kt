package com.atelbay.money_manager.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.atelbay.money_manager.core.database.entity.RegexParserProfileEntity

@Dao
interface RegexParserProfileDao {
    @Query("SELECT * FROM parser_configs WHERE status = 'active' AND configType = :configType")
    suspend fun getActiveByType(configType: String): List<RegexParserProfileEntity>

    @Query("SELECT * FROM parser_configs WHERE status = 'active'")
    suspend fun getAllActive(): List<RegexParserProfileEntity>

    @Query("SELECT * FROM parser_configs WHERE bankId = :bankId AND configType = :configType AND status = 'active' LIMIT 1")
    suspend fun getByBankIdAndType(bankId: String, configType: String): RegexParserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(configs: List<RegexParserProfileEntity>)

    @Query("DELETE FROM parser_configs WHERE id NOT IN (:activeIds)")
    suspend fun deleteStale(activeIds: List<String>)

    @Query("DELETE FROM parser_configs WHERE id NOT IN (:activeIds) AND source NOT IN ('ai_cached')")
    suspend fun deleteStaleExceptLocal(activeIds: List<String>)

    @Query("DELETE FROM parser_configs")
    suspend fun deleteAll()
}
