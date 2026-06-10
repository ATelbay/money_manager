package com.atelbay.money_manager.core.model

import kotlinx.datetime.LocalDate

data class ParsedTransaction(
    val date: LocalDate,
    val amount: Long,
    val type: TransactionType,
    val operationType: String = "",
    val details: String,
    val categoryId: Long?,
    val suggestedCategoryName: String?,
    /**
     * Category name resolved during parsing but not yet persisted. Created on import (within the
     * import transaction), so a previewed-then-cancelled statement leaves no orphan categories.
     */
    val pendingCategoryName: String? = null,
    val confidence: Float,
    val needsReview: Boolean,
    val uniqueHash: String,
)
