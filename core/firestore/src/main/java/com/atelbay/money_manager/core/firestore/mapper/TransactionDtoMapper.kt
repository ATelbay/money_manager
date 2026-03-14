package com.atelbay.money_manager.core.firestore.mapper

import com.atelbay.money_manager.core.crypto.FieldCipher.Companion.CURRENT_ENCRYPTION_VERSION
import com.atelbay.money_manager.core.crypto.FieldCipherHolder
import com.atelbay.money_manager.core.database.entity.TransactionEntity
import com.atelbay.money_manager.core.firestore.dto.TransactionDto
import timber.log.Timber
import java.security.GeneralSecurityException
import java.util.UUID

fun TransactionEntity.toDto(
    categoryRemoteId: String,
    accountRemoteId: String,
    fieldCipherHolder: FieldCipherHolder,
): TransactionDto {
    val cipher = fieldCipherHolder.cipher
    return TransactionDto(
        remoteId = remoteId ?: UUID.randomUUID().toString(),
        amount = if (cipher != null) cipher.encryptDouble(amount) else amount.toString(),
        type = type,
        categoryRemoteId = categoryRemoteId,
        accountRemoteId = accountRemoteId,
        note = if (cipher != null) note?.let { cipher.encrypt(it) } else note,
        date = date,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
        uniqueHash = if (cipher != null) uniqueHash?.let { cipher.encrypt(it) } else uniqueHash,
        encryptionVersion = if (cipher != null) CURRENT_ENCRYPTION_VERSION else 0,
    )
}

fun TransactionDto.toEntity(
    localId: Long = 0,
    categoryLocalId: Long,
    accountLocalId: Long,
    fieldCipherHolder: FieldCipherHolder,
): TransactionEntity? {
    val cipher = fieldCipherHolder.cipher
    if (encryptionVersion >= 1 && cipher == null) {
        Timber.w("Cannot decrypt TransactionDto: cipher unavailable (encryptionVersion=$encryptionVersion)")
        return null
    }
    return try {
        if (encryptionVersion == CURRENT_ENCRYPTION_VERSION && cipher != null) {
            TransactionEntity(
                id = localId,
                remoteId = remoteId,
                amount = cipher.decryptDouble(amount),
                type = type,
                categoryId = categoryLocalId,
                accountId = accountLocalId,
                note = note?.let { cipher.decrypt(it) },
                date = date,
                createdAt = createdAt,
                updatedAt = updatedAt,
                isDeleted = isDeleted,
                uniqueHash = uniqueHash?.let { cipher.decrypt(it) },
            )
        } else {
            TransactionEntity(
                id = localId,
                remoteId = remoteId,
                amount = amount.toDouble(),
                type = type,
                categoryId = categoryLocalId,
                accountId = accountLocalId,
                note = note,
                date = date,
                createdAt = createdAt,
                updatedAt = updatedAt,
                isDeleted = isDeleted,
                uniqueHash = uniqueHash,
            )
        }
    } catch (e: GeneralSecurityException) {
        Timber.e(e, "Failed to decrypt transaction %s", remoteId)
        null
    } catch (e: IllegalArgumentException) {
        Timber.e(e, "Failed to decrypt transaction %s", remoteId)
        null
    }
}
