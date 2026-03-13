# Tasks: Client-Side Firestore Encryption

**Input**: Design documents from `/specs/007-firestore-encryption/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md

**Tests**: Not explicitly requested in spec — test tasks included only for the new `core:crypto` module (foundational, high-risk cryptographic code).

**Organization**: Tasks grouped by user story. US1 (encrypt push) and US2+US3 (decrypt pull + legacy) are separated into distinct phases since they touch different code paths.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup

**Purpose**: Add Tink dependency and create `core:crypto` module skeleton

- [ ] T001 Add `tink-android = "1.8.0"` version and library entry to `gradle/libs.versions.toml`
- [ ] T002 Add `include(":core:crypto")` to `settings.gradle.kts`
- [ ] T003 Create `core/crypto/build.gradle.kts` with `moneymanager.android.library` + `moneymanager.android.hilt` plugins, dependency on `libs.tink.android` (no `core:auth` dependency needed — UID is passed as a String parameter to `FieldCipherHolder.init()`)

---

## Phase 2: Foundational (FieldCipher)

**Purpose**: Implement the encryption primitive that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T004 Create `FieldCipher` interface in `core/crypto/src/main/java/com/atelbay/money_manager/core/crypto/FieldCipher.kt` with methods: `encrypt(plaintext: String): String`, `decrypt(ciphertext: String): String`, `encryptDouble(value: Double): String`, `decryptDouble(ciphertext: String): Double`, `encryptLong(value: Long): String`, `decryptLong(ciphertext: String): Long`
- [ ] T005 Create `AesGcmFieldCipher` implementation in `core/crypto/src/main/java/com/atelbay/money_manager/core/crypto/AesGcmFieldCipher.kt` — derive 256-bit AES key via `Hkdf.computeHkdf("HmacSha256", uid.toByteArray(), SALT, INFO, 32)`, use `AesGcmJce(key)` for encrypt/decrypt, Base64-encode ciphertext with `Base64.NO_WRAP`
- [ ] T006 Create `FieldCipherHolder` class and `CryptoModule` Hilt DI in `core/crypto/src/main/java/com/atelbay/money_manager/core/crypto/` — `FieldCipherHolder` is a mutable container (`var cipher: FieldCipher? = null`) with `fun init(uid: String)` (derives key, creates AesGcmFieldCipher) and `fun clear()` (zeros key bytes, nulls cipher). `CryptoModule`: `@Module @InstallIn(SingletonComponent)`, `@Provides @Singleton` for `FieldCipherHolder`. Mappers receive `FieldCipherHolder` and read `.cipher` at call time
- [ ] T007 Create unit tests in `core/crypto/src/test/java/com/atelbay/money_manager/core/crypto/AesGcmFieldCipherTest.kt` — test cases: String encrypt/decrypt round-trip, Double encrypt/decrypt round-trip, Long encrypt/decrypt round-trip, same UID produces same key (determinism), different UIDs produce different keys (isolation), decrypt with wrong key throws exception, encrypted output is valid Base64

**Checkpoint**: FieldCipher is functional and tested — user story implementation can begin

---

## Phase 3: User Story 1 — Sensitive Data Encrypted in Firestore (Priority: P1) 🎯 MVP

**Goal**: All sensitive financial fields are encrypted before pushing to Firestore. Firebase Console shows Base64 ciphertext for amounts, names, balances, notes, icons, colors.

**Independent Test**: Create a transaction/account/category, trigger sync, open Firebase Console and verify sensitive fields are unreadable Base64 strings while metadata (IDs, timestamps, type, currency) remains plaintext.

### Implementation for User Story 1

- [ ] T008 [P] [US1] Modify `TransactionDto` in `core/firestore/src/main/java/com/atelbay/money_manager/core/firestore/dto/TransactionDto.kt` — change `amount: Double` to `amount: String = ""`, add `val encryptionVersion: Int = 0`
- [ ] T009 [P] [US1] Modify `AccountDto` in `core/firestore/src/main/java/com/atelbay/money_manager/core/firestore/dto/AccountDto.kt` — change `balance: Double` to `balance: String = ""`, add `val encryptionVersion: Int = 0`
- [ ] T010 [P] [US1] Modify `CategoryDto` in `core/firestore/src/main/java/com/atelbay/money_manager/core/firestore/dto/CategoryDto.kt` — change `color: Long` to `color: String = ""`, add `val encryptionVersion: Int = 0`
- [ ] T019 [US1+US3] Update `FirestoreDataSourceImpl` pull methods in `core/firestore/src/main/java/com/atelbay/money_manager/core/firestore/datasource/FirestoreDataSourceImpl.kt` — replace `toObject(Dto::class.java)` with `document.data` map-based DTO construction for `pullTransactions`, `pullAccounts`, `pullCategories` to handle type polymorphism (legacy `amount`/`balance` as Double vs encrypted String, legacy `color` as Long vs encrypted String). **⚠️ MUST land together with T008-T010** — without map-based deserialization, `toObject()` will crash on legacy Firestore documents after DTO type changes.
- [ ] T014 [US1] Add `implementation(projects.core.crypto)` to `core/firestore/build.gradle.kts`. **⚠️ MUST precede T011-T013** — mappers import `FieldCipherHolder` from `core:crypto`.
- [ ] T011 [P] [US1] Update `TransactionDtoMapper.toDto()` in `core/firestore/src/main/java/com/atelbay/money_manager/core/firestore/mapper/TransactionDtoMapper.kt` — add `fieldCipherHolder: FieldCipherHolder` parameter, encrypt `amount` (via `encryptDouble`), `note`, `uniqueHash` when `holder.cipher` is non-null, set `encryptionVersion = 1`. Depends on T014.
- [ ] T012 [P] [US1] Update `AccountDtoMapper.toDto()` in `core/firestore/src/main/java/com/atelbay/money_manager/core/firestore/mapper/AccountDtoMapper.kt` — add `fieldCipherHolder: FieldCipherHolder` parameter, encrypt `name`, `balance` (via `encryptDouble`) when `holder.cipher` is non-null, set `encryptionVersion = 1`. Depends on T014.
- [ ] T013 [P] [US1] Update `CategoryDtoMapper.toDto()` in `core/firestore/src/main/java/com/atelbay/money_manager/core/firestore/mapper/CategoryDtoMapper.kt` — add `fieldCipherHolder: FieldCipherHolder` parameter, encrypt `name`, `icon`, `color` (via `encryptLong`) when `holder.cipher` is non-null, set `encryptionVersion = 1`. Depends on T014.
- [ ] T015 [US1] Update `SyncManager` in `data/sync/src/main/java/com/atelbay/money_manager/data/sync/SyncManager.kt` — inject `FieldCipherHolder` via constructor, pass to all `.toDto()` mapper calls in `syncTransaction`, `syncAccount`, `syncCategory`, `pushAllAccounts`, `pushAllPending`

**Checkpoint**: Push path encrypts all sensitive fields. Firebase Console shows ciphertext. Pull deserialization updated to handle both legacy Double/Long and encrypted String fields via map-based construction.

---

## Phase 4: User Story 2 + User Story 3 — Cross-Device Decryption & Legacy Migration (Priority: P1 + P2)

**Goal**: Encrypted data pulled from Firestore is decrypted correctly on any device with the same Google account. Legacy unencrypted documents (encryptionVersion=0) are read as plaintext without error.

**Independent Test (US2)**: Sign in on Device A, create data, sync. Sign in on Device B with the same Google account, verify all data appears correctly.

**Independent Test (US3)**: Pre-populate Firestore with unencrypted documents (no encryptionVersion field), install updated app, sign in, verify data loads correctly.

### Implementation for User Story 2 + 3

- [ ] T016 [P] [US2] Update `TransactionDtoMapper.toEntity()` in `core/firestore/src/main/java/com/atelbay/money_manager/core/firestore/mapper/TransactionDtoMapper.kt` — add `fieldCipherHolder: FieldCipherHolder` parameter, if `encryptionVersion == 1` decrypt `amount` (via `decryptDouble`), `note`, `uniqueHash`; if `encryptionVersion == 0` parse `amount` as plaintext Double string
- [ ] T017 [P] [US2] Update `AccountDtoMapper.toEntity()` in `core/firestore/src/main/java/com/atelbay/money_manager/core/firestore/mapper/AccountDtoMapper.kt` — add `fieldCipherHolder: FieldCipherHolder` parameter, if `encryptionVersion == 1` decrypt `name`, `balance` (via `decryptDouble`); if `encryptionVersion == 0` parse `balance` as plaintext Double string
- [ ] T018 [P] [US2] Update `CategoryDtoMapper.toEntity()` in `core/firestore/src/main/java/com/atelbay/money_manager/core/firestore/mapper/CategoryDtoMapper.kt` — add `fieldCipherHolder: FieldCipherHolder` parameter, if `encryptionVersion == 1` decrypt `name`, `icon`, `color` (via `decryptLong`); if `encryptionVersion == 0` parse `color` as plaintext Long string
- [ ] T020 [US2] Update `PullSyncUseCase` in `data/sync/src/main/java/com/atelbay/money_manager/data/sync/PullSyncUseCase.kt` — inject `FieldCipherHolder` via constructor, pass to all `.toEntity()` mapper calls
- [ ] T021 [US2] Add key lifecycle management — in `LoginSyncOrchestrator` (`data/sync/src/main/java/com/atelbay/money_manager/data/sync/LoginSyncOrchestrator.kt`): call `fieldCipherHolder.init(uid)` after successful sign-in, call `fieldCipherHolder.clear()` on sign-out. Verify `clear()` zeros the key bytes and nulls the cipher reference so no key material persists after logout

**Checkpoint**: Full round-trip works — push encrypts, pull decrypts. Legacy unencrypted documents are handled gracefully. Cross-device sync works with the same Google account.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Build verification and edge case handling

- [ ] T022 Add decryption error handling (try/catch) in all mapper `toEntity` methods — on `GeneralSecurityException` or `IllegalArgumentException`, log error via Timber and skip the document per FR-012
- [ ] T023 [P] Create `TransactionDtoMapperTest` in `core/firestore/src/test/` — test cases: `toDto` encrypts amount/note/uniqueHash and sets `encryptionVersion=1`, `toEntity` decrypts back to original values (round-trip), `toEntity` with `encryptionVersion=0` reads plaintext Double amount, `toEntity` with null note/uniqueHash preserves nulls, `toEntity` with corrupted ciphertext throws or returns null per FR-012
- [ ] T024 [P] Create `AccountDtoMapperTest` in `core/firestore/src/test/` — test cases: `toDto` encrypts name/balance and sets `encryptionVersion=1`, `toEntity` decrypts back (round-trip), `toEntity` with `encryptionVersion=0` reads plaintext Double balance, `toDto` with null cipher skips encryption
- [ ] T025 [P] Create `CategoryDtoMapperTest` in `core/firestore/src/test/` — test cases: `toDto` encrypts name/icon/color and sets `encryptionVersion=1`, `toEntity` decrypts back (round-trip), `toEntity` with `encryptionVersion=0` reads plaintext Long color, `toDto` with null cipher skips encryption
- [ ] T026 Run full build verification: `./gradlew assembleDebug test lint detekt` — fix any compilation errors from DTO type changes across all modules. Known affected call sites: `core/firestore/mapper/TransactionDtoMapper.kt`, `core/firestore/mapper/AccountDtoMapper.kt`, `core/firestore/mapper/CategoryDtoMapper.kt`, `data/sync/SyncManager.kt`, `data/sync/PullSyncUseCase.kt`
- [ ] T027 [P] Add performance benchmark test in `core/crypto/src/test/` — encrypt + decrypt 500 String fields and 500 Double fields, assert total time ≤100ms (validates SC-004)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 — BLOCKS all user stories
- **US1 (Phase 3)**: Depends on Phase 2 — push path only
- **US2+US3 (Phase 4)**: Depends on Phase 3 (mapper toDto changes must exist before adding toEntity changes to same files)
- **Polish (Phase 5)**: Depends on Phase 4

### User Story Dependencies

- **US1 (P1)**: Can start after Foundational (Phase 2) — encrypts push path
- **US2 (P1)**: Depends on US1 completion (same mapper files, adds decrypt to existing encrypt code)
- **US3 (P2)**: Implemented alongside US2 (legacy handling is part of the decrypt path)

### Within Each Phase

- T008, T009, T010, T019 can run in parallel (different files; T019 MUST land together with DTO type changes)
- T014 must precede T011, T012, T013 (build dependency before mapper code using FieldCipherHolder)
- T011, T012, T013 can run in parallel (different mapper files, after T014)
- T016, T017, T018 can run in parallel (different mapper files, toEntity method)
- T015 after T014 + mappers (SyncManager wires everything together)

### Parallel Opportunities

```
Phase 2:   T004 → T005 → T006 (sequential: interface → impl → DI)
           T007 (after T005)

Phase 3:   T008 ─┐
           T009 ─┼─ parallel DTO changes + deserialization fix
           T010 ─┤
           T019 ─┘  (map-based deserialization, MUST land with DTO type changes)
           T014 → T011 ─┐
                  T012 ─┼─ parallel mapper encrypt (after T014 build dep)
                  T013 ─┘
           T015 (SyncManager, after T014 + mappers)

Phase 4:   T016 ─┐
           T017 ─┼─ parallel mapper decrypt
           T018 ─┘
           T020 (PullSyncUseCase, after mappers)
           T021 (key lifecycle, independent)

Phase 5:   T022, T023-T025 [P], T026, T027 [P]
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001-T003)
2. Complete Phase 2: Foundational FieldCipher (T004-T007)
3. Complete Phase 3: US1 — encrypted push (T008-T015)
4. **STOP and VALIDATE**: Push data, check Firebase Console for encrypted fields
5. Data is encrypted in Firestore — core privacy guarantee met

### Full Delivery

1. Setup + Foundational → FieldCipher ready
2. US1 → Encrypted push → Validate in Firebase Console
3. US2+US3 → Decrypt pull + legacy → Validate cross-device + legacy migration
4. Polish → Error handling + build verification
5. Each phase adds value without breaking previous work

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- `FieldCipherHolder` is a `@Singleton` mutable container — `holder.cipher` is null when user is not signed in, mappers skip encryption/decryption, maintaining offline-first behavior
- ParserCandidateDto is explicitly excluded — no changes to parser candidate push/pull
- Room entities are never modified — encryption is transparent at the Firestore sync boundary only
- Commit after each task or logical group of parallel tasks
