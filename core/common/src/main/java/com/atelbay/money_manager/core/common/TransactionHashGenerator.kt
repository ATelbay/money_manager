package com.atelbay.money_manager.core.common

import kotlinx.datetime.LocalDate
import java.security.MessageDigest

fun generateTransactionHash(
    date: LocalDate,
    amount: Double,
    type: String,
    details: String,
): String {
    val input = "$date|$amount|$type|${details.take(30)}"
    val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}
