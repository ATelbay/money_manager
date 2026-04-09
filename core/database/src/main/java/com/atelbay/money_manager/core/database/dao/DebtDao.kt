package com.atelbay.money_manager.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.atelbay.money_manager.core.database.entity.DebtEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {

    @Query("SELECT * FROM debts WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE id = :id AND isDeleted = 0")
    suspend fun getById(id: Long): DebtEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DebtEntity): Long

    @Update
    suspend fun update(entity: DebtEntity)

    @Query("UPDATE debts SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteById(id: Long, updatedAt: Long)

    // ── Sync ──

    @Query("SELECT * FROM debts WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): DebtEntity?

    @Query("SELECT * FROM debts WHERE remoteId IS NULL AND isDeleted = 0")
    suspend fun getPendingSync(): List<DebtEntity>

    @Query("SELECT * FROM debts WHERE isDeleted = 1 AND remoteId IS NOT NULL")
    suspend fun getDeletedWithRemoteId(): List<DebtEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSync(debts: List<DebtEntity>)

    @Query("UPDATE debts SET remoteId = NULL")
    suspend fun clearRemoteIds()
}
