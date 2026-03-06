package com.atelbay.money_manager.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.atelbay.money_manager.core.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories WHERE isDeleted = 0 ORDER BY name ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE isDeleted = 0 AND type = :type ORDER BY name ASC")
    fun observeByType(type: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    fun observeById(id: Long): Flow<CategoryEntity?>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Query("SELECT * FROM categories WHERE isDeleted = 0 AND type = :type ORDER BY name ASC")
    suspend fun getByType(type: String): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("UPDATE categories SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteById(id: Long, updatedAt: Long)

    // ── Sync ──

    @Query("SELECT * FROM categories WHERE isDeleted = 0 AND isDefault = 0")
    suspend fun getAllNonDefaultForSync(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE remoteId IS NULL AND isDeleted = 0 AND isDefault = 0")
    suspend fun getPendingSync(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSync(categories: List<CategoryEntity>)
}
