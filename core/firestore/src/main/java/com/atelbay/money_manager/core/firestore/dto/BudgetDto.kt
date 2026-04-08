package com.atelbay.money_manager.core.firestore.dto

import com.google.firebase.firestore.DocumentId

data class BudgetDto(
    @DocumentId val remoteId: String = "",
    val categoryRemoteId: String = "",
    val monthlyLimit: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val isDeleted: Boolean = false,
    val encryptionVersion: Int = 0,
)
