package com.atelbay.money_manager.core.firestore.mapper

import com.atelbay.money_manager.core.crypto.FieldCipher.Companion.CURRENT_ENCRYPTION_VERSION
import com.atelbay.money_manager.core.crypto.FieldCipherHolder
import com.atelbay.money_manager.core.database.entity.BudgetEntity
import com.atelbay.money_manager.core.firestore.dto.BudgetDto
import timber.log.Timber
import java.security.GeneralSecurityException
import java.util.UUID

fun BudgetEntity.toDto(
    fieldCipherHolder: FieldCipherHolder,
    categoryRemoteId: String,
): BudgetDto {
    val cipher = fieldCipherHolder.cipher
    return BudgetDto(
        remoteId = remoteId ?: UUID.randomUUID().toString(),
        categoryRemoteId = categoryRemoteId,
        monthlyLimit = if (cipher != null) cipher.encryptDouble(monthlyLimit) else monthlyLimit.toString(),
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
        encryptionVersion = if (cipher != null) CURRENT_ENCRYPTION_VERSION else 0,
    )
}

fun BudgetDto.toEntity(
    localId: Long = 0,
    fieldCipherHolder: FieldCipherHolder,
    localCategoryId: Long,
): BudgetEntity? {
    val cipher = fieldCipherHolder.cipher
    if (encryptionVersion >= 1 && cipher == null) {
        Timber.w("Cannot decrypt BudgetDto: cipher unavailable (encryptionVersion=$encryptionVersion)")
        return null
    }
    return try {
        if (encryptionVersion == CURRENT_ENCRYPTION_VERSION && cipher != null) {
            BudgetEntity(
                id = localId,
                remoteId = remoteId,
                categoryId = localCategoryId,
                monthlyLimit = cipher.decryptDouble(monthlyLimit),
                createdAt = createdAt,
                updatedAt = updatedAt,
                isDeleted = isDeleted,
            )
        } else if (encryptionVersion > 0) {
            Timber.w("Unsupported encryption version %d for budget %s", encryptionVersion, remoteId)
            return null
        } else {
            val limit = monthlyLimit.toDoubleOrNull()
            if (limit == null) {
                Timber.w("Invalid monthlyLimit '%s' for budget %s", monthlyLimit, remoteId)
                return null
            }
            BudgetEntity(
                id = localId,
                remoteId = remoteId,
                categoryId = localCategoryId,
                monthlyLimit = limit,
                createdAt = createdAt,
                updatedAt = updatedAt,
                isDeleted = isDeleted,
            )
        }
    } catch (e: GeneralSecurityException) {
        Timber.e(e, "Failed to decrypt budget %s", remoteId)
        null
    } catch (e: IllegalArgumentException) {
        Timber.e(e, "Failed to decrypt budget %s", remoteId)
        null
    }
}
