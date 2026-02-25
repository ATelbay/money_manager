package com.atelbay.money_manager.data.transactions.mapper

import com.atelbay.money_manager.core.database.entity.CategoryEntity
import com.atelbay.money_manager.core.database.entity.TransactionEntity
import com.atelbay.money_manager.core.model.Transaction
import com.atelbay.money_manager.core.model.TransactionType

fun TransactionEntity.toDomain(category: CategoryEntity?): Transaction = Transaction(
    id = id,
    amount = amount,
    type = TransactionType.fromValue(type),
    categoryId = categoryId,
    categoryName = category?.name.orEmpty(),
    categoryIcon = category?.icon.orEmpty(),
    categoryColor = category?.color ?: 0xFF90A4AE,
    accountId = accountId,
    note = note,
    date = date,
    createdAt = createdAt,
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    amount = amount,
    type = type.value,
    categoryId = categoryId,
    accountId = accountId,
    note = note,
    date = date,
    createdAt = createdAt,
)
