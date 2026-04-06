package com.atelbay.money_manager.core.firestore.mapper

import com.atelbay.money_manager.core.crypto.FieldCipher.Companion.CURRENT_ENCRYPTION_VERSION
import com.atelbay.money_manager.core.crypto.FieldCipherHolder
import com.atelbay.money_manager.core.database.entity.RecurringTransactionEntity
import com.atelbay.money_manager.core.firestore.dto.RecurringTransactionDto
import timber.log.Timber
import java.security.GeneralSecurityException
import java.util.UUID

fun RecurringTransactionEntity.toDto(
    categoryRemoteId: String,
    accountRemoteId: String,
    fieldCipherHolder: FieldCipherHolder,
): RecurringTransactionDto {
    val cipher = fieldCipherHolder.cipher
    return RecurringTransactionDto(
        remoteId = remoteId ?: UUID.randomUUID().toString(),
        amount = if (cipher != null) cipher.encryptDouble(amount) else amount.toString(),
        type = type,
        categoryRemoteId = categoryRemoteId,
        accountRemoteId = accountRemoteId,
        note = if (cipher != null) note?.let { cipher.encrypt(it) } else note,
        frequency = frequency,
        startDate = startDate,
        endDate = endDate,
        dayOfMonth = dayOfMonth,
        dayOfWeek = dayOfWeek,
        lastGeneratedDate = lastGeneratedDate,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
        encryptionVersion = if (cipher != null) CURRENT_ENCRYPTION_VERSION else 0,
    )
}

fun RecurringTransactionDto.toEntity(
    localId: Long = 0,
    categoryLocalId: Long,
    accountLocalId: Long,
    fieldCipherHolder: FieldCipherHolder,
): RecurringTransactionEntity? {
    val cipher = fieldCipherHolder.cipher
    if (encryptionVersion >= 1 && cipher == null) {
        Timber.w("Cannot decrypt RecurringTransactionDto: cipher unavailable (encryptionVersion=$encryptionVersion)")
        return null
    }
    return try {
        if (encryptionVersion == CURRENT_ENCRYPTION_VERSION && cipher != null) {
            RecurringTransactionEntity(
                id = localId,
                remoteId = remoteId,
                amount = cipher.decryptDouble(amount),
                type = type,
                categoryId = categoryLocalId,
                accountId = accountLocalId,
                note = note?.let { cipher.decrypt(it) },
                frequency = frequency,
                startDate = startDate,
                endDate = endDate,
                dayOfMonth = dayOfMonth,
                dayOfWeek = dayOfWeek,
                lastGeneratedDate = lastGeneratedDate,
                isActive = isActive,
                createdAt = createdAt,
                updatedAt = updatedAt,
                isDeleted = isDeleted,
            )
        } else {
            RecurringTransactionEntity(
                id = localId,
                remoteId = remoteId,
                amount = amount.toDouble(),
                type = type,
                categoryId = categoryLocalId,
                accountId = accountLocalId,
                note = note,
                frequency = frequency,
                startDate = startDate,
                endDate = endDate,
                dayOfMonth = dayOfMonth,
                dayOfWeek = dayOfWeek,
                lastGeneratedDate = lastGeneratedDate,
                isActive = isActive,
                createdAt = createdAt,
                updatedAt = updatedAt,
                isDeleted = isDeleted,
            )
        }
    } catch (e: GeneralSecurityException) {
        Timber.e(e, "Failed to decrypt recurring transaction %s", remoteId)
        null
    } catch (e: IllegalArgumentException) {
        Timber.e(e, "Failed to decrypt recurring transaction %s", remoteId)
        null
    }
}
