package com.atelbay.money_manager.core.firestore.mapper

import com.atelbay.money_manager.core.database.entity.AccountEntity
import com.atelbay.money_manager.core.firestore.dto.AccountDto
import java.util.UUID

fun AccountEntity.toDto(): AccountDto = AccountDto(
    remoteId = remoteId ?: UUID.randomUUID().toString(),
    name = name,
    currency = currency,
    balance = balance,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
)

fun AccountDto.toEntity(localId: Long = 0): AccountEntity = AccountEntity(
    id = localId,
    remoteId = remoteId,
    name = name,
    currency = currency,
    balance = balance,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
)
