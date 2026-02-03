package com.atelbay.money_manager.core.model

import kotlinx.datetime.LocalDate

data class ParsedTransaction(
    val date: LocalDate,
    val amount: Double,
    val type: TransactionType,
    val operationType: String,
    val details: String,
    val categoryId: Long?,
    val suggestedCategoryName: String?,
    val confidence: Float,
    val needsReview: Boolean,
    val uniqueHash: String,
)
