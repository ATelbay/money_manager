package com.atelbay.money_manager.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.atelbay.money_manager.core.database.entity.ParserConfigEntity

@Dao
interface ParserConfigDao {
    @Query("SELECT * FROM parser_configs WHERE status = 'active' AND configType = :configType")
    suspend fun getActiveByType(configType: String): List<ParserConfigEntity>

    @Query("SELECT * FROM parser_configs WHERE status = 'active'")
    suspend fun getAllActive(): List<ParserConfigEntity>

    @Query("SELECT * FROM parser_configs WHERE bankId = :bankId AND configType = :configType AND status = 'active' LIMIT 1")
    suspend fun getByBankIdAndType(bankId: String, configType: String): ParserConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(configs: List<ParserConfigEntity>)

    @Query("DELETE FROM parser_configs WHERE id NOT IN (:activeIds)")
    suspend fun deleteStale(activeIds: List<String>)

    @Query("DELETE FROM parser_configs")
    suspend fun deleteAll()
}
