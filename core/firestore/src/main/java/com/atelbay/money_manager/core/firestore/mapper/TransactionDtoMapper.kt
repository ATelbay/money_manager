package com.atelbay.money_manager.core.firestore.mapper

import com.atelbay.money_manager.core.database.entity.TransactionEntity
import com.atelbay.money_manager.core.firestore.dto.TransactionDto
import java.util.UUID

fun TransactionEntity.toDto(categoryRemoteId: String, accountRemoteId: String): TransactionDto =
    TransactionDto(
        remoteId = remoteId ?: UUID.randomUUID().toString(),
        amount = amount,
        type = type,
        categoryRemoteId = categoryRemoteId,
        accountRemoteId = accountRemoteId,
        note = note,
        date = date,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
        uniqueHash = uniqueHash,
    )

fun TransactionDto.toEntity(localId: Long = 0, categoryLocalId: Long, accountLocalId: Long): TransactionEntity =
    TransactionEntity(
        id = localId,
        remoteId = remoteId,
        amount = amount,
        type = type,
        categoryId = categoryLocalId,
        accountId = accountLocalId,
        note = note,
        date = date,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
        uniqueHash = uniqueHash,
    )
