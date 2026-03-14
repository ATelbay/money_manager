# Quickstart: Client-Side Firestore Encryption

**Branch**: `007-firestore-encryption` | **Date**: 2026-03-13

## New Module

### `core/crypto`

```
core/crypto/
├── build.gradle.kts                    # moneymanager.android.library + moneymanager.android.hilt
└── src/
    ├── main/java/com/atelbay/money_manager/core/crypto/
    │   ├── FieldCipher.kt              # Interface: encrypt/decrypt strings, doubles, longs
    │   ├── AesGcmFieldCipher.kt        # Tink AesGcmJce implementation
    │   └── di/
    │       └── CryptoModule.kt         # Hilt @Module providing FieldCipher
    └── test/java/com/atelbay/money_manager/core/crypto/
        ├── AesGcmFieldCipherTest.kt    # Encrypt/decrypt round-trip, null handling, wrong key
        └── KeyDerivationTest.kt        # HKDF determinism, different UIDs → different keys
```

**Convention plugin**: `moneymanager.android.library` + `moneymanager.android.hilt`
**Dependencies**: `com.google.crypto.tink:tink-android:1.8.0`, `core:auth` (for UID access)

## Modified Files

### `core/firestore/`

| File | Change |
|------|--------|
| `dto/TransactionDto.kt` | `amount: Double→String`, add `encryptionVersion: Int = 0` |
| `dto/AccountDto.kt` | `balance: Double→String`, add `encryptionVersion: Int = 0` |
| `dto/CategoryDto.kt` | `color: Long→String`, add `encryptionVersion: Int = 0` |
| `mapper/TransactionDtoMapper.kt` | Accept `FieldCipher?`, encrypt on toDto, decrypt on toEntity |
| `mapper/AccountDtoMapper.kt` | Accept `FieldCipher?`, encrypt on toDto, decrypt on toEntity |
| `mapper/CategoryDtoMapper.kt` | Accept `FieldCipher?`, encrypt on toDto, decrypt on toEntity |
| `datasource/FirestoreDataSourceImpl.kt` | Pull methods: use `document.data` map instead of `toObject()` for migration |
| `build.gradle.kts` | Add dependency on `core:crypto` |

### `data/sync/`

| File | Change |
|------|--------|
| `SyncManager.kt` | Inject `FieldCipher`, pass to mapper calls |
| `PullSyncUseCase.kt` | Inject `FieldCipher`, pass to mapper calls |
| `build.gradle.kts` | Add dependency on `core:crypto` (if not transitive) |

### Build files

| File | Change |
|------|--------|
| `gradle/libs.versions.toml` | Add `tink-android = "1.8.0"` version + library entry |
| `settings.gradle.kts` | Add `include(":core:crypto")` |

## Build & Verify

```bash
# After creating core/crypto module and modifying files:
./gradlew :core:crypto:test           # Unit tests for FieldCipher
./gradlew :core:firestore:test        # Mapper tests with encryption
./gradlew :data:sync:test             # Sync integration tests
./gradlew assembleDebug                # Full build verification
```

## Key Integration Pattern

```kotlin
// In SyncManager (push path):
val dto = entity.toDto(categoryRemoteId, accountRemoteId, fieldCipher)
firestoreDataSource.pushTransaction(userId, dto)

// In PullSyncUseCase (pull path):
val dtos = firestoreDataSource.pullTransactions(userId)
for (dto in dtos) {
    val entity = dto.toEntity(localId, categoryLocalId, accountLocalId, fieldCipher)
    // ... merge logic unchanged
}
```
