package com.atelbay.money_manager.core.model

data class Account(
    val id: Long = 0,
    val name: String,
    val currency: String,
    val balance: Long,
    val createdAt: Long = 0,
)
