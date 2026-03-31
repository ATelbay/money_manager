package com.atelbay.money_manager.core.firestore.dto

import com.google.firebase.firestore.DocumentId

data class ParserConfigFirestoreDto(
    @DocumentId val id: String = "",
    val bankId: String = "",
    val configType: String = "",
    val configJson: String = "",
    val version: Long = 0,
    val status: String = "active",
    val source: String = "",
    val updatedAt: Long = 0,
    val candidateId: String? = null,
)
