package com.atelbay.money_manager.core.firestore.mapper

import com.atelbay.money_manager.core.crypto.FieldCipher.Companion.CURRENT_ENCRYPTION_VERSION
import com.atelbay.money_manager.core.crypto.FieldCipherHolder
import com.atelbay.money_manager.core.database.entity.DebtEntity
import com.atelbay.money_manager.core.firestore.dto.DebtDto
import com.atelbay.money_manager.core.model.money.majorToMinor
import com.atelbay.money_manager.core.model.money.toMajorDouble
import com.atelbay.money_manager.core.model.money.toMajorPlainString
import timber.log.Timber
import java.security.GeneralSecurityException
import java.util.UUID

fun DebtEntity.toDto(
    fieldCipherHolder: FieldCipherHolder,
    accountRemoteId: String,
): DebtDto {
    val cipher = fieldCipherHolder.cipher
    return DebtDto(
        remoteId = remoteId ?: UUID.randomUUID().toString(),
        contactName = if (cipher != null) cipher.encrypt(contactName) else contactName,
        direction = direction,
        totalAmount = if (cipher != null) cipher.encryptDouble(totalAmount.toMajorDouble()) else totalAmount.toMajorPlainString(),
        totalAmountMinor = if (cipher != null) cipher.encryptLong(totalAmount) else totalAmount.toString(),
        currency = currency,
        accountRemoteId = accountRemoteId,
        note = if (cipher != null) cipher.encrypt(note ?: "") else (note ?: ""),
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
        encryptionVersion = if (cipher != null) CURRENT_ENCRYPTION_VERSION else 0,
    )
}

fun DebtDto.toEntity(
    localId: Long = 0,
    fieldCipherHolder: FieldCipherHolder,
    localAccountId: Long,
): DebtEntity? {
    val cipher = fieldCipherHolder.cipher
    if (encryptionVersion >= 1 && cipher == null) {
        Timber.w("Cannot decrypt DebtDto: cipher unavailable (encryptionVersion=$encryptionVersion)")
        return null
    }
    return try {
        if (encryptionVersion == CURRENT_ENCRYPTION_VERSION && cipher != null) {
            DebtEntity(
                id = localId,
                remoteId = remoteId,
                contactName = cipher.decrypt(contactName),
                direction = direction,
                totalAmount = totalAmountMinor?.takeIf { it.isNotBlank() }?.let { cipher.decryptLong(it) }
                    ?: majorToMinor(cipher.decryptDouble(totalAmount)),
                currency = currency,
                accountId = localAccountId,
                note = cipher.decrypt(note).ifEmpty { null },
                createdAt = createdAt,
                updatedAt = updatedAt,
                isDeleted = isDeleted,
            )
        } else if (encryptionVersion > 0) {
            Timber.w("Unsupported encryption version %d for debt %s", encryptionVersion, remoteId)
            return null
        } else {
            val amount = totalAmountMinor?.toLongOrNull() ?: totalAmount.toDoubleOrNull()?.let { majorToMinor(it) }
            if (amount == null) {
                Timber.w("Invalid totalAmount '%s' for debt %s", totalAmount, remoteId)
                return null
            }
            DebtEntity(
                id = localId,
                remoteId = remoteId,
                contactName = contactName,
                direction = direction,
                totalAmount = amount,
                currency = currency,
                accountId = localAccountId,
                note = note.ifEmpty { null },
                createdAt = createdAt,
                updatedAt = updatedAt,
                isDeleted = isDeleted,
            )
        }
    } catch (e: GeneralSecurityException) {
        Timber.e(e, "Failed to decrypt debt %s", remoteId)
        null
    } catch (e: IllegalArgumentException) {
        Timber.e(e, "Failed to decrypt debt %s", remoteId)
        null
    }
}
