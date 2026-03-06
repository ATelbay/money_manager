package com.atelbay.money_manager.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.atelbay.money_manager.core.database.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Query("SELECT * FROM accounts WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    fun observeById(id: Long): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity): Long

    @Update
    suspend fun update(account: AccountEntity)

    @Delete
    suspend fun delete(account: AccountEntity)

    @Query("UPDATE accounts SET balance = balance + :delta, updatedAt = :updatedAt WHERE id = :accountId")
    suspend fun updateBalance(accountId: Long, delta: Double, updatedAt: Long)

    @Query("UPDATE accounts SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteById(id: Long, updatedAt: Long)

    // ── Sync ──

    @Query("SELECT * FROM accounts WHERE isDeleted = 0")
    suspend fun getAllForSync(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE remoteId IS NULL AND isDeleted = 0")
    suspend fun getPendingSync(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSync(accounts: List<AccountEntity>)
}
