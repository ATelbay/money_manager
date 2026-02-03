package com.atelbay.money_manager.core.model

import kotlinx.datetime.LocalDate

data class TransactionOverride(
    val amount: Double? = null,
    val type: TransactionType? = null,
    val details: String? = null,
    val date: LocalDate? = null,
    val categoryId: Long? = null,
)
