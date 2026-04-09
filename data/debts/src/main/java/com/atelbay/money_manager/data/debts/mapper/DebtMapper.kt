package com.atelbay.money_manager.data.debts.mapper

import com.atelbay.money_manager.core.database.entity.DebtEntity
import com.atelbay.money_manager.core.model.Debt
import com.atelbay.money_manager.core.model.DebtDirection
import com.atelbay.money_manager.core.model.DebtStatus

fun DebtEntity.toDomain(paidAmount: Double, accountName: String): Debt {
    val remaining = totalAmount - paidAmount
    return Debt(
        id = id,
        contactName = contactName,
        direction = DebtDirection.valueOf(direction),
        totalAmount = totalAmount,
        paidAmount = paidAmount,
        remainingAmount = remaining.coerceAtLeast(0.0),
        currency = currency,
        accountId = accountId,
        accountName = accountName,
        note = note,
        createdAt = createdAt,
        status = if (remaining <= 0) DebtStatus.PAID_OFF else DebtStatus.ACTIVE,
    )
}

fun Debt.toEntity(): DebtEntity = DebtEntity(
    id = id,
    contactName = contactName,
    direction = direction.name,
    totalAmount = totalAmount,
    currency = currency,
    accountId = accountId,
    note = note,
    createdAt = createdAt,
)
