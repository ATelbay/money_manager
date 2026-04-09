package com.atelbay.money_manager.core.firestore.dto

import com.google.firebase.firestore.DocumentId

data class DebtPaymentDto(
    @DocumentId val remoteId: String = "",
    val debtRemoteId: String = "",
    val amount: String = "",
    val date: Long = 0,
    val note: String = "",
    val transactionRemoteId: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val isDeleted: Boolean = false,
    val encryptionVersion: Int = 0,
)
