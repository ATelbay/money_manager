package com.atelbay.money_manager.core.model

data class ImportResult(
    val total: Int,
    val newTransactions: List<ParsedTransaction>,
    val duplicates: Int,
    val errors: List<String>,
)
