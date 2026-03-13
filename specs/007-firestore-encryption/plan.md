# Implementation Plan: Client-Side Firestore Encryption

**Branch**: `007-firestore-encryption` | **Date**: 2026-03-13 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/007-firestore-encryption/spec.md`

## Summary

Add client-side field-level AES-256-GCM encryption for sensitive financial data (amounts, names, balances, notes, icons, colors) before pushing to Firestore. Key derived deterministically from Firebase UID via HKDF-SHA256, enabling cross-device decryption with the same Google account. Non-sensitive metadata (IDs, timestamps, type, currency, deletion flags) remains plaintext for queries and sync. A new `core:crypto` module provides `FieldCipher` interface with Tink implementation. Encryption integrates at the mapper layer (`core:firestore/mapper/`), transparent to SyncManager and PullSyncUseCase.

## Technical Context

**Language/Version**: Kotlin 2.3.0
**Primary Dependencies**: Google Tink Android 1.8.0 (`AesGcmJce`, `Hkdf`), Firebase Firestore (via BOM 34.8.0), Hilt 2.58
**Storage**: Room 2.8.4 (unchanged), Firestore (encrypted DTOs)
**Testing**: JUnit 4, MockK 1.14.9, Turbine 1.2.1, kotlinx-coroutines-test
**Target Platform**: Android (minSdk 29, compileSdk 36)
**Project Type**: Mobile app (layer-centric multi-module)
**Performance Goals**: ≤100ms encryption/decryption overhead per sync batch (up to 500 documents)
**Constraints**: Offline-first (Room is source of truth), no Room entity changes, key in-memory only
**Scale/Scope**: Personal finance app, ~1000 documents per user max

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Clean Architecture Multi-Module | ✅ Pass | New `core:crypto` module follows core/ layer convention. No new domain/data/presentation modules — encryption is infrastructure. |
| II. Kotlin-First & Jetpack Compose | ✅ Pass | All code in Kotlin. No UI changes. |
| III. Material 3 Design System | ✅ N/A | No UI changes. |
| IV. Animation & Motion | ✅ N/A | No UI changes. |
| V. Hilt Dependency Injection | ✅ Pass | `FieldCipher` provided via Hilt `@Module`/`@Binds` in `core:crypto`. `@Singleton` scope appropriate for key cache. |
| VI. Room Database | ✅ Pass | No Room entity changes (FR-007). Encryption is transparent at Firestore boundary. |
| VII. Testing Architecture | ✅ Pass | Unit tests for FieldCipher (round-trip, wrong key, null handling), mapper tests with encryption. |
| VIII. Firebase Ecosystem | ✅ Pass | Integrates with existing Firestore sync. No new Firebase services. |
| IX. Type-Safe Navigation | ✅ N/A | No navigation changes. |
| X. Statement Import Pipeline | ✅ N/A | No import changes. |
| XI. Preferences DataStore | ✅ N/A | Key stored in-memory, not DataStore. |

**Gate result**: ALL PASS — no violations.

## Project Structure

### Documentation (this feature)

```text
specs/007-firestore-encryption/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0: Tink research, key derivation, migration strategy
├── data-model.md        # Phase 1: DTO changes, FieldCipher entity
├── quickstart.md        # Phase 1: Module setup, build commands
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
core/crypto/                                    # NEW MODULE
├── build.gradle.kts
└── src/
    ├── main/java/com/atelbay/money_manager/core/crypto/
    │   ├── FieldCipher.kt                      # Interface
    │   ├── AesGcmFieldCipher.kt                # Tink implementation
    │   └── di/CryptoModule.kt                  # Hilt bindings
    └── test/java/com/atelbay/money_manager/core/crypto/
        ├── AesGcmFieldCipherTest.kt            # Round-trip, error handling
        └── KeyDerivationTest.kt                # Determinism, isolation

core/firestore/                                 # MODIFIED
├── src/main/java/.../dto/
│   ├── TransactionDto.kt                       # amount→String, +encryptionVersion
│   ├── AccountDto.kt                           # balance→String, +encryptionVersion
│   └── CategoryDto.kt                          # color→String, +encryptionVersion
├── src/main/java/.../mapper/
│   ├── TransactionDtoMapper.kt                 # +FieldCipher param, encrypt/decrypt
│   ├── AccountDtoMapper.kt                     # +FieldCipher param, encrypt/decrypt
│   └── CategoryDtoMapper.kt                    # +FieldCipher param, encrypt/decrypt
├── src/main/java/.../datasource/
│   └── FirestoreDataSourceImpl.kt              # Pull: Map-based deserialization
└── build.gradle.kts                            # +dependency core:crypto

data/sync/                                      # MODIFIED
├── src/main/java/.../
│   ├── SyncManager.kt                          # Inject FieldCipher, pass to mappers
│   └── PullSyncUseCase.kt                      # Inject FieldCipher, pass to mappers
└── build.gradle.kts                            # +dependency core:crypto (if needed)

gradle/libs.versions.toml                       # +tink-android entry
settings.gradle.kts                             # +include(":core:crypto")
```

**Structure Decision**: Follows existing layer-centric pattern. `core:crypto` is a new infrastructure module (like `core:auth`, `core:firestore`). No new domain/data/presentation modules because encryption is an infrastructure concern at the Firestore boundary, not a user-facing feature.

**FieldCipher Lifecycle**: `CryptoModule` provides a `@Singleton FieldCipherHolder` — a mutable container with `var cipher: FieldCipher? = null`. On sign-in, the holder derives the key from the current UID and sets `cipher`. On sign-out, `LoginSyncOrchestrator` calls `holder.clear()` which nulls the reference and zeros the key bytes. Mappers receive `FieldCipherHolder` and read `.cipher` at call time. This avoids Hilt scope issues — the singleton is the holder, not the cipher itself.

## Complexity Tracking

No constitution violations — no entries needed.
