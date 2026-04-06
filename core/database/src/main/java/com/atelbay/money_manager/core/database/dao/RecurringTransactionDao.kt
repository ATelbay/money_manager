package com.atelbay.money_manager.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.atelbay.money_manager.core.database.entity.RecurringTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {

    @Query("SELECT * FROM recurring_transactions WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE id = :id AND isDeleted = 0")
    suspend fun getById(id: Long): RecurringTransactionEntity?

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 AND isDeleted = 0")
    suspend fun getActiveRecurrings(): List<RecurringTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RecurringTransactionEntity): Long

    @Update
    suspend fun update(entity: RecurringTransactionEntity)

    @Query("UPDATE recurring_transactions SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: Long, updatedAt: Long)

    @Query("UPDATE recurring_transactions SET lastGeneratedDate = :date, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateLastGeneratedDate(id: Long, date: Long, updatedAt: Long)

    // ── Sync ──

    @Query("SELECT * FROM recurring_transactions WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): RecurringTransactionEntity?

    @Query("SELECT * FROM recurring_transactions WHERE remoteId IS NULL AND isDeleted = 0")
    suspend fun getPendingSync(): List<RecurringTransactionEntity>

    @Query("SELECT * FROM recurring_transactions WHERE isDeleted = 1 AND remoteId IS NOT NULL")
    suspend fun getDeletedWithRemoteId(): List<RecurringTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSync(recurringTransactions: List<RecurringTransactionEntity>)

    @Query("UPDATE recurring_transactions SET remoteId = NULL")
    suspend fun clearRemoteIds()

    @Query("UPDATE recurring_transactions SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteById(id: Long, updatedAt: Long)
}
