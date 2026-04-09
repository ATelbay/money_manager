package com.atelbay.money_manager.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "debt_payments",
    foreignKeys = [
        ForeignKey(
            entity = DebtEntity::class,
            parentColumns = ["id"],
            childColumns = ["debtId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("debtId"),
        Index("remoteId"),
        Index("transactionId"),
    ],
)
data class DebtPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtId: Long,
    val amount: Double,
    val date: Long,
    val note: String? = null,
    val transactionId: Long? = null,
    val createdAt: Long,
    val remoteId: String? = null,
    val updatedAt: Long = 0,
    val isDeleted: Boolean = false,
)
