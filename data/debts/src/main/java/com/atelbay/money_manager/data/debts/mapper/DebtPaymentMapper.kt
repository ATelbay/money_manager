package com.atelbay.money_manager.data.debts.mapper

import com.atelbay.money_manager.core.database.entity.DebtPaymentEntity
import com.atelbay.money_manager.core.model.DebtPayment

fun DebtPaymentEntity.toDomain(): DebtPayment = DebtPayment(
    id = id,
    debtId = debtId,
    amount = amount,
    date = date,
    note = note,
    transactionId = transactionId,
    createdAt = createdAt,
)

fun DebtPayment.toEntity(): DebtPaymentEntity = DebtPaymentEntity(
    id = id,
    debtId = debtId,
    amount = amount,
    date = date,
    note = note,
    transactionId = transactionId,
    createdAt = createdAt,
)
