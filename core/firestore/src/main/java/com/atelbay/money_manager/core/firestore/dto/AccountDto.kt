package com.atelbay.money_manager.core.firestore.dto

import com.google.firebase.firestore.DocumentId

data class AccountDto(
    @DocumentId val remoteId: String = "",
    val name: String = "",
    val currency: String = "",
    val balance: Double = 0.0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val isDeleted: Boolean = false,
)
