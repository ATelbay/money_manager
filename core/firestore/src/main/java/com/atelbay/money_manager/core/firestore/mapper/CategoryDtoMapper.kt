package com.atelbay.money_manager.core.firestore.mapper

import com.atelbay.money_manager.core.crypto.FieldCipher.Companion.CURRENT_ENCRYPTION_VERSION
import com.atelbay.money_manager.core.crypto.FieldCipherHolder
import com.atelbay.money_manager.core.database.entity.CategoryEntity
import com.atelbay.money_manager.core.firestore.dto.CategoryDto
import timber.log.Timber
import java.security.GeneralSecurityException
import java.util.UUID

fun CategoryEntity.toDto(fieldCipherHolder: FieldCipherHolder): CategoryDto {
    val cipher = fieldCipherHolder.cipher
    return CategoryDto(
        remoteId = remoteId ?: UUID.randomUUID().toString(),
        name = if (cipher != null) cipher.encrypt(name) else name,
        icon = if (cipher != null) cipher.encrypt(icon) else icon,
        color = if (cipher != null) cipher.encryptLong(color) else color.toString(),
        type = type,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
        encryptionVersion = if (cipher != null) CURRENT_ENCRYPTION_VERSION else 0,
    )
}

fun CategoryDto.toEntity(localId: Long = 0, fieldCipherHolder: FieldCipherHolder): CategoryEntity? {
    val cipher = fieldCipherHolder.cipher
    if (encryptionVersion >= 1 && cipher == null) {
        Timber.w("Cannot decrypt CategoryDto: cipher unavailable (encryptionVersion=$encryptionVersion)")
        return null
    }
    return try {
        if (encryptionVersion == CURRENT_ENCRYPTION_VERSION && cipher != null) {
            CategoryEntity(
                id = localId,
                remoteId = remoteId,
                name = cipher.decrypt(name),
                icon = cipher.decrypt(icon),
                color = cipher.decryptLong(color),
                type = type,
                isDefault = false,
                updatedAt = updatedAt,
                isDeleted = isDeleted,
            )
        } else {
            CategoryEntity(
                id = localId,
                remoteId = remoteId,
                name = name,
                icon = icon,
                color = color.toLong(),
                type = type,
                isDefault = false,
                updatedAt = updatedAt,
                isDeleted = isDeleted,
            )
        }
    } catch (e: GeneralSecurityException) {
        Timber.e(e, "Failed to decrypt category %s", remoteId)
        null
    } catch (e: IllegalArgumentException) {
        Timber.e(e, "Failed to decrypt category %s", remoteId)
        null
    }
}
