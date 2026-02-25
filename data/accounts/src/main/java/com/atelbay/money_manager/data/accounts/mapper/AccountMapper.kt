package com.atelbay.money_manager.data.accounts.mapper

import com.atelbay.money_manager.core.database.entity.AccountEntity
import com.atelbay.money_manager.core.model.Account

fun AccountEntity.toDomain(): Account = Account(
    id = id,
    name = name,
    currency = currency,
    balance = balance,
    createdAt = createdAt,
)

fun Account.toEntity(): AccountEntity = AccountEntity(
    id = id,
    name = name,
    currency = currency,
    balance = balance,
    createdAt = createdAt,
)
