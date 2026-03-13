package com.atelbay.money_manager.core.firestore.mapper

import com.atelbay.money_manager.core.crypto.FieldCipher.Companion.CURRENT_ENCRYPTION_VERSION
import com.atelbay.money_manager.core.crypto.FieldCipherHolder
import com.atelbay.money_manager.core.database.entity.AccountEntity
import com.atelbay.money_manager.core.firestore.dto.AccountDto
import timber.log.Timber
import java.security.GeneralSecurityException
import java.util.UUID

fun AccountEntity.toDto(fieldCipherHolder: FieldCipherHolder): AccountDto {
    val cipher = fieldCipherHolder.cipher
    return AccountDto(
        remoteId = remoteId ?: UUID.randomUUID().toString(),
        name = if (cipher != null) cipher.encrypt(name) else name,
        currency = currency,
        balance = if (cipher != null) cipher.encryptDouble(balance) else balance.toString(),
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
        encryptionVersion = if (cipher != null) CURRENT_ENCRYPTION_VERSION else 0,
    )
}

fun AccountDto.toEntity(localId: Long = 0, fieldCipherHolder: FieldCipherHolder): AccountEntity? {
    val cipher = fieldCipherHolder.cipher
    if (encryptionVersion >= 1 && cipher == null) {
        Timber.w("Cannot decrypt AccountDto: cipher unavailable (encryptionVersion=$encryptionVersion)")
        return null
    }
    return try {
        if (encryptionVersion == CURRENT_ENCRYPTION_VERSION && cipher != null) {
            AccountEntity(
                id = localId,
                remoteId = remoteId,
                name = cipher.decrypt(name),
                currency = currency,
                balance = cipher.decryptDouble(balance),
                createdAt = createdAt,
                updatedAt = updatedAt,
                isDeleted = isDeleted,
            )
        } else {
            AccountEntity(
                id = localId,
                remoteId = remoteId,
                name = name,
                currency = currency,
                balance = balance.toDouble(),
                createdAt = createdAt,
                updatedAt = updatedAt,
                isDeleted = isDeleted,
            )
        }
    } catch (e: GeneralSecurityException) {
        Timber.e(e, "Failed to decrypt account %s", remoteId)
        null
    } catch (e: IllegalArgumentException) {
        Timber.e(e, "Failed to decrypt account %s", remoteId)
        null
    }
}
