package com.atelbay.money_manager.data.recurring.mapper

import com.atelbay.money_manager.core.database.entity.RecurringTransactionEntity
import com.atelbay.money_manager.core.model.Frequency
import com.atelbay.money_manager.core.model.RecurringTransaction
import com.atelbay.money_manager.core.model.TransactionType

fun RecurringTransactionEntity.toDomain(
    categoryName: String,
    categoryIcon: String,
    categoryColor: Long,
    accountName: String,
): RecurringTransaction = RecurringTransaction(
    id = id,
    amount = amount,
    type = TransactionType.fromValue(type),
    categoryId = categoryId,
    categoryName = categoryName,
    categoryIcon = categoryIcon,
    categoryColor = categoryColor,
    accountId = accountId,
    accountName = accountName,
    note = note,
    frequency = Frequency.valueOf(frequency),
    startDate = startDate,
    endDate = endDate,
    dayOfMonth = dayOfMonth,
    dayOfWeek = dayOfWeek,
    lastGeneratedDate = lastGeneratedDate,
    isActive = isActive,
    createdAt = createdAt,
)

fun RecurringTransaction.toEntity(): RecurringTransactionEntity = RecurringTransactionEntity(
    id = id,
    amount = amount,
    type = type.value,
    categoryId = categoryId,
    accountId = accountId,
    note = note,
    frequency = frequency.name,
    startDate = startDate,
    endDate = endDate,
    dayOfMonth = dayOfMonth,
    dayOfWeek = dayOfWeek,
    lastGeneratedDate = lastGeneratedDate,
    isActive = isActive,
    createdAt = createdAt,
)
