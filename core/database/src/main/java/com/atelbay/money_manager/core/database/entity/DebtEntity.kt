package com.atelbay.money_manager.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "debts",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("accountId"),
        Index("remoteId"),
    ],
)
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactName: String,
    val direction: String,
    val totalAmount: Double,
    val currency: String,
    val accountId: Long,
    val note: String? = null,
    val createdAt: Long,
    val remoteId: String? = null,
    val updatedAt: Long = 0,
    val isDeleted: Boolean = false,
)
