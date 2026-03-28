package com.atelbay.money_manager.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recurring_transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("accountId"),
        Index("categoryId"),
        Index("isActive", "isDeleted"),
    ],
)
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: String,
    val categoryId: Long,
    val accountId: Long,
    val note: String?,
    val frequency: String,
    val startDate: Long,
    val endDate: Long?,
    val dayOfMonth: Int?,
    val dayOfWeek: Int?,
    val lastGeneratedDate: Long?,
    val isActive: Boolean = true,
    val createdAt: Long,
    val remoteId: String? = null,
    val updatedAt: Long = 0,
    val isDeleted: Boolean = false,
)
