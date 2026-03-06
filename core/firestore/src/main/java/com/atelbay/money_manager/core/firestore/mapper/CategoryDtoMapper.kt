package com.atelbay.money_manager.core.firestore.mapper

import com.atelbay.money_manager.core.database.entity.CategoryEntity
import com.atelbay.money_manager.core.firestore.dto.CategoryDto
import java.util.UUID

fun CategoryEntity.toDto(): CategoryDto = CategoryDto(
    remoteId = remoteId ?: UUID.randomUUID().toString(),
    name = name,
    icon = icon,
    color = color,
    type = type,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
)

fun CategoryDto.toEntity(localId: Long = 0): CategoryEntity = CategoryEntity(
    id = localId,
    remoteId = remoteId,
    name = name,
    icon = icon,
    color = color,
    type = type,
    isDefault = false,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
)
