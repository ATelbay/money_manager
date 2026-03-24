package com.atelbay.money_manager.core.firestore.dto

import com.google.firebase.firestore.DocumentId

data class ParserCandidateDto(
    @DocumentId val id: String = "",
    val bankId: String = "",
    val transactionPattern: String = "",
    val parserConfigJson: String = "",
    val anonymizedSample: String = "",
    val userIdHash: String = "",
    val successCount: Int = 0,
    val status: String = "candidate",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val configType: String = "regex",
)
