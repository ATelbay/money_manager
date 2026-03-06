package com.atelbay.money_manager.core.firestore.dto

import com.google.firebase.firestore.DocumentId

data class CategoryDto(
    @DocumentId val remoteId: String = "",
    val name: String = "",
    val icon: String = "",
    val color: Long = 0,
    val type: String = "",
    val updatedAt: Long = 0,
    val isDeleted: Boolean = false,
)
