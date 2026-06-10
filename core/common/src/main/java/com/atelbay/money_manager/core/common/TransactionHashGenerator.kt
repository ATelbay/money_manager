package com.atelbay.money_manager.core.common

import kotlinx.datetime.LocalDate
import java.security.MessageDigest

fun generateTransactionHash(
    date: LocalDate,
    amountMinor: Long,
    type: String,
    details: String,
): String {
    // Reconstruct the legacy major-unit string so the digest stays stable across app versions and
    // across the Double→Long migration (research R1). For all clean <=2-dp amounts (the universe of
    // real input) `(amountMinor / 100.0).toString()` reproduces the original Double's string form.
    val amount = amountMinor / 100.0
    val input = "$date|$amount|$type|${details.take(30)}"
    val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}
