package com.atelbay.money_manager.core.firestore.dto

import com.google.firebase.firestore.DocumentId

data class AccountDto(
    @DocumentId val remoteId: String = "",
    val name: String = "",
    val currency: String = "",
    val balance: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val isDeleted: Boolean = false,
    val encryptionVersion: Int = 0,
)
