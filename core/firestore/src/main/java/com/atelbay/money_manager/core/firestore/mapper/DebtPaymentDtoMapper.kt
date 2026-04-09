package com.atelbay.money_manager.core.firestore.mapper

import com.atelbay.money_manager.core.crypto.FieldCipher.Companion.CURRENT_ENCRYPTION_VERSION
import com.atelbay.money_manager.core.crypto.FieldCipherHolder
import com.atelbay.money_manager.core.database.entity.DebtPaymentEntity
import com.atelbay.money_manager.core.firestore.dto.DebtPaymentDto
import timber.log.Timber
import java.security.GeneralSecurityException
import java.util.UUID

fun DebtPaymentEntity.toDto(
    fieldCipherHolder: FieldCipherHolder,
    debtRemoteId: String,
    transactionRemoteId: String,
): DebtPaymentDto {
    val cipher = fieldCipherHolder.cipher
    return DebtPaymentDto(
        remoteId = remoteId ?: UUID.randomUUID().toString(),
        debtRemoteId = debtRemoteId,
        amount = if (cipher != null) cipher.encryptDouble(amount) else amount.toString(),
        date = date,
        note = if (cipher != null) cipher.encrypt(note ?: "") else (note ?: ""),
        transactionRemoteId = transactionRemoteId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
        encryptionVersion = if (cipher != null) CURRENT_ENCRYPTION_VERSION else 0,
    )
}

fun DebtPaymentDto.toEntity(
    localId: Long = 0,
    fieldCipherHolder: FieldCipherHolder,
    localDebtId: Long,
    localTransactionId: Long?,
): DebtPaymentEntity? {
    val cipher = fieldCipherHolder.cipher
    if (encryptionVersion >= 1 && cipher == null) {
        Timber.w("Cannot decrypt DebtPaymentDto: cipher unavailable (encryptionVersion=$encryptionVersion)")
        return null
    }
    return try {
        if (encryptionVersion == CURRENT_ENCRYPTION_VERSION && cipher != null) {
            DebtPaymentEntity(
                id = localId,
                remoteId = remoteId,
                debtId = localDebtId,
                amount = cipher.decryptDouble(amount),
                date = date,
                note = cipher.decrypt(note).ifEmpty { null },
                transactionId = localTransactionId,
                createdAt = createdAt,
                updatedAt = updatedAt,
                isDeleted = isDeleted,
            )
        } else if (encryptionVersion > 0) {
            Timber.w("Unsupported encryption version %d for debt payment %s", encryptionVersion, remoteId)
            return null
        } else {
            val parsedAmount = amount.toDoubleOrNull()
            if (parsedAmount == null) {
                Timber.w("Invalid amount '%s' for debt payment %s", amount, remoteId)
                return null
            }
            DebtPaymentEntity(
                id = localId,
                remoteId = remoteId,
                debtId = localDebtId,
                amount = parsedAmount,
                date = date,
                note = note.ifEmpty { null },
                transactionId = localTransactionId,
                createdAt = createdAt,
                updatedAt = updatedAt,
                isDeleted = isDeleted,
            )
        }
    } catch (e: GeneralSecurityException) {
        Timber.e(e, "Failed to decrypt debt payment %s", remoteId)
        null
    } catch (e: IllegalArgumentException) {
        Timber.e(e, "Failed to decrypt debt payment %s", remoteId)
        null
    }
}
