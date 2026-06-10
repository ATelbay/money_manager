package com.atelbay.money_manager.core.model

import kotlinx.datetime.LocalDate

data class TransactionOverride(
    val amount: Long? = null,
    val type: TransactionType? = null,
    val details: String? = null,
    val date: LocalDate? = null,
    val categoryId: Long? = null,
)
