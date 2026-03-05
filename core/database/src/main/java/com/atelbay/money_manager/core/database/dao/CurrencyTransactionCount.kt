package com.atelbay.money_manager.core.database.dao

/**
 * Room query result POJO: currency code with the number of transactions
 * associated with accounts in that currency.
 */
data class CurrencyTransactionCount(
    val currency: String,
    val transactionCount: Int,
)
