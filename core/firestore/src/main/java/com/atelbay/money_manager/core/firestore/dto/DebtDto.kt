package com.atelbay.money_manager.core.firestore.dto

import com.google.firebase.firestore.DocumentId

data class DebtDto(
    @DocumentId val remoteId: String = "",
    val contactName: String = "",
    val direction: String = "",
    val totalAmount: String = "",
    val currency: String = "",
    val accountRemoteId: String = "",
    val note: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val isDeleted: Boolean = false,
    val encryptionVersion: Int = 0,
)
