package com.atelbay.money_manager.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.atelbay.money_manager.core.database.entity.DebtPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtPaymentDao {

    @Query("SELECT * FROM debt_payments WHERE isDeleted = 0 ORDER BY date DESC")
    fun observeAll(): Flow<List<DebtPaymentEntity>>

    @Query("SELECT * FROM debt_payments WHERE debtId = :debtId AND isDeleted = 0 ORDER BY date DESC")
    fun observeByDebtId(debtId: Long): Flow<List<DebtPaymentEntity>>

    @Query("SELECT * FROM debt_payments WHERE debtId = :debtId AND isDeleted = 0 ORDER BY date DESC")
    suspend fun getByDebtId(debtId: Long): List<DebtPaymentEntity>

    @Query("SELECT * FROM debt_payments WHERE id = :id AND isDeleted = 0")
    suspend fun getById(id: Long): DebtPaymentEntity?

    @Query("SELECT SUM(amount) FROM debt_payments WHERE debtId = :debtId AND isDeleted = 0")
    fun sumAmountByDebtId(debtId: Long): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DebtPaymentEntity): Long

    @Update
    suspend fun update(entity: DebtPaymentEntity)

    @Query("UPDATE debt_payments SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteById(id: Long, updatedAt: Long)

    @Query("UPDATE debt_payments SET isDeleted = 1, updatedAt = :updatedAt WHERE debtId = :debtId AND isDeleted = 0")
    suspend fun softDeleteByDebtId(debtId: Long, updatedAt: Long)

    // ── Sync ──

    @Query("SELECT * FROM debt_payments WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): DebtPaymentEntity?

    @Query("SELECT * FROM debt_payments WHERE remoteId IS NULL AND isDeleted = 0")
    suspend fun getPendingSync(): List<DebtPaymentEntity>

    @Query("SELECT * FROM debt_payments WHERE isDeleted = 1 AND remoteId IS NOT NULL")
    suspend fun getDeletedWithRemoteId(): List<DebtPaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSync(payments: List<DebtPaymentEntity>)

    @Query("UPDATE debt_payments SET remoteId = NULL")
    suspend fun clearRemoteIds()
}
