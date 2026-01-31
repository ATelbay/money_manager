package com.atelbay.money_manager.core.model

enum class TransactionType(val value: String) {
    INCOME("income"),
    EXPENSE("expense");

    companion object {
        fun fromValue(value: String): TransactionType =
            entries.first { it.value == value }
    }
}
