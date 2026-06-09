package com.atelbay.money_manager.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.atelbay.money_manager.core.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY date DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun observeByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND accountId = :accountId ORDER BY date DESC")
    fun observeByAccount(accountId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND categoryId = :categoryId ORDER BY date DESC")
    fun observeByCategory(categoryId: Long): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE isDeleted = 0
          AND categoryId = :categoryId
          AND type = :type
          AND date BETWEEN :startDate AND :endDate
        ORDER BY date DESC
        """,
    )
    fun observeByCategoryTypeAndDateRange(
        categoryId: Long,
        type: String,
        startDate: Long,
        endDate: Long,
    ): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id AND isDeleted = 0")
    fun observeById(id: Long): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    /**
     * Inserts a single transaction. Uses ABORT (not REPLACE) on conflict so a `uniqueHash`
     * collision fails loudly instead of silently delete+reinserting the row — which would change
     * its id and fire ON DELETE SET NULL on linked debt_payments. Batch import dedups via
     * [insertOrIgnore]; manual saves carry a null hash and never conflict.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE transactions SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteById(id: Long, updatedAt: Long)

    @Query("UPDATE transactions SET isDeleted = 1, updatedAt = :updatedAt WHERE accountId = :accountId AND isDeleted = 0")
    suspend fun softDeleteByAccountId(accountId: Long, updatedAt: Long)

    /** Active (non-deleted) transaction ids for an account — captured before a cascade soft-delete so each can be synced. */
    @Query("SELECT id FROM transactions WHERE accountId = :accountId AND isDeleted = 0")
    suspend fun getIdsByAccountId(accountId: Long): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(transactions: List<TransactionEntity>): List<Long>

    /**
     * Inserts a single transaction, ignoring a `uniqueHash` collision (returns -1 then). Used by
     * recurring generation so re-running / a cross-device pull of the same occurrence is idempotent
     * and never double-counts the account balance.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(transaction: TransactionEntity): Long

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE uniqueHash = :hash)")
    suspend fun existsByHash(hash: String): Boolean

    @Query("SELECT uniqueHash FROM transactions WHERE uniqueHash IN (:hashes)")
    suspend fun getExistingHashes(hashes: List<String>): List<String>

    /**
     * Returns currency codes ranked by all-time transaction count (descending).
     * Ties are broken alphabetically by currency code for deterministic ordering.
     */
    @Query(
        """
        SELECT a.currency, COUNT(t.id) AS transactionCount
        FROM transactions t
        INNER JOIN accounts a ON t.accountId = a.id
        GROUP BY a.currency
        ORDER BY transactionCount DESC, a.currency ASC
        """,
    )
    suspend fun getCurrencyTransactionCounts(): List<CurrencyTransactionCount>

    // ── Sync ──

    @Query("SELECT * FROM transactions WHERE isDeleted = 0")
    suspend fun getAllForSync(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE remoteId IS NULL AND isDeleted = 0")
    suspend fun getPendingSync(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE isDeleted = 1")
    suspend fun getDeletedForSync(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSync(transactions: List<TransactionEntity>)

    @Query("UPDATE transactions SET remoteId = NULL")
    suspend fun clearRemoteIds()

    @Query("SELECT * FROM transactions WHERE uniqueHash = :hash LIMIT 1")
    suspend fun getByUniqueHash(hash: String): TransactionEntity?

    /** Wipes all transactions. Used when wiping local data on a user switch (see SyncManager). */
    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE categoryId = :categoryId AND type = 'expense' AND isDeleted = 0 AND date >= :startDate AND date < :endDate")
    fun observeExpenseSumByCategory(categoryId: Long, startDate: Long, endDate: Long): Flow<Long>
}
