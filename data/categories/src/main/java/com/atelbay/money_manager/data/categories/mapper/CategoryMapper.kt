package com.atelbay.money_manager.data.categories.mapper

import com.atelbay.money_manager.core.database.entity.CategoryEntity
import com.atelbay.money_manager.core.model.Category
import com.atelbay.money_manager.core.model.TransactionType

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    icon = icon,
    color = color,
    type = TransactionType.fromValue(type),
    isDefault = isDefault,
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    icon = icon,
    color = color,
    type = type.value,
    isDefault = isDefault,
)
