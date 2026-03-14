package com.atelbay.money_manager.core.firestore.dto

import com.google.firebase.firestore.DocumentId

data class TransactionDto(
    @DocumentId val remoteId: String = "",
    val amount: String = "",
    val type: String = "",
    val categoryRemoteId: String = "",
    val accountRemoteId: String = "",
    val note: String? = null,
    val date: Long = 0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val isDeleted: Boolean = false,
    val uniqueHash: String? = null,
    val encryptionVersion: Int = 0,
)
