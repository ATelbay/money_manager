package com.atelbay.money_manager.core.firestore.dto

import com.google.firebase.firestore.DocumentId

data class RecurringTransactionDto(
    @DocumentId val remoteId: String = "",
    val amount: String = "",
    val type: String = "",
    val categoryRemoteId: String = "",
    val accountRemoteId: String = "",
    val note: String? = null,
    val frequency: String = "",
    val startDate: Long = 0,
    val endDate: Long? = null,
    val dayOfMonth: Int? = null,
    val dayOfWeek: Int? = null,
    val lastGeneratedDate: Long? = null,
    val isActive: Boolean = true,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val isDeleted: Boolean = false,
    val encryptionVersion: Int = 0,
)
