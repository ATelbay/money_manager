package com.atelbay.money_manager.core.firestore.dto

import com.google.firebase.firestore.DocumentId

data class CategoryDto(
    @DocumentId val remoteId: String = "",
    val name: String = "",
    val icon: String = "",
    val color: String = "",
    val type: String = "",
    val updatedAt: Long = 0,
    val isDeleted: Boolean = false,
    val encryptionVersion: Int = 0,
)
