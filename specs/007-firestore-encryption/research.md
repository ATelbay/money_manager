# Research: Client-Side Firestore Encryption

**Branch**: `007-firestore-encryption` | **Date**: 2026-03-13

## R-001: Encryption Library — Google Tink Android

**Decision**: Use `com.google.crypto.tink:tink-android:1.8.0`

**Rationale**: Tink provides both HKDF key derivation (`Hkdf.computeHkdf`) and AES-256-GCM encryption (`AesGcmJce`) in a single library. The `subtle` package offers low-level primitives that accept raw key bytes — essential for our UID-derived key flow. Tink 1.8.0 is the latest stable Android release (original repo is in maintenance mode, development continues at `tink-crypto/tink-java`).

**Alternatives considered**:
- `javax.crypto.Cipher` (JCE): Manual IV/tag management, more boilerplate, higher error surface
- `tink-java` (non-Android): Lacks Android-specific optimizations; `tink-android` is the correct artifact
- Bouncy Castle: Heavier, unnecessary for AES-GCM which is natively supported

## R-002: Key Derivation — HKDF-SHA256

**Decision**: Use `Hkdf.computeHkdf("HmacSha256", uid.toByteArray(), salt, info, 32)` to derive a 256-bit AES key from the Firebase UID.

**Rationale**: Firebase UIDs are 28-char server-generated alphanumeric strings (~166 bits of entropy) — sufficient input key material for HKDF. HKDF is deterministic: same UID + salt + info → same key on any device. No need for PBKDF2/Argon2 (those are for low-entropy passwords).

**Parameters**:
- `ikm`: Firebase UID bytes (UTF-8)
- `salt`: Fixed 32-byte app-specific constant (hardcoded in source)
- `info`: Context string `"money-manager-firestore-v1"` (domain separation)
- `output size`: 32 bytes (AES-256)

**Alternatives considered**:
- PBKDF2: Designed for passwords (slow by design), unnecessary overhead for high-entropy UIDs
- Raw UID as key: UID is only 28 chars; HKDF stretches it to proper key length with cryptographic guarantees

## R-003: AES-GCM Encrypt/Decrypt — AesGcmJce

**Decision**: Use `AesGcmJce(derivedKey)` for field-level encryption. One instance per user session (thread-safe).

**Rationale**: `AesGcmJce` handles IV generation automatically — each `encrypt()` call generates a random 12-byte IV internally. Output format: `IV (12 bytes) || ciphertext || GCM tag (16 bytes)`. The `decrypt()` method extracts IV from the first 12 bytes automatically.

**Key properties**:
- Thread-safe: fresh random IV per `encrypt()` call
- No manual IV management needed
- Output overhead: 28 bytes (12 IV + 16 tag) per encrypted field, + ~33% Base64 expansion
- Associated data (AAD): empty byte array (field binding not needed; fields already tied to document)

## R-004: DTO Type Migration for Encrypted Fields

**Decision**: Change encrypted DTO field types from `Double`/`Long` to `String`. Pull uses `document.data` (Map) instead of `toObject()` to handle legacy documents with original types.

**Rationale**: Encrypted fields are always Base64 strings. Legacy documents may still have `amount: 50000.0` (Double in Firestore). Firestore's `toObject()` cannot handle polymorphic field types (sometimes Double, sometimes String). Using `document.data` map allows manual type checking and conversion based on `encryptionVersion`.

**Affected DTO fields**:
- `TransactionDto.amount`: `Double` → `String` (was `50000.0`, becomes `"Base64..."`)
- `AccountDto.balance`: `Double` → `String` (same pattern)
- `CategoryDto.color`: `Long` → `String` (was `0xFF4CAF50`, becomes `"Base64..."`)
- String fields (`name`, `note`, `uniqueHash`, `icon`): type unchanged, value becomes encrypted

**Migration strategy**:
- Push: always encrypt (version=1), all fields are String
- Pull: check `encryptionVersion`. If 0/missing, cast numeric fields from their original types to String representations, then use as plaintext

## R-005: Encryption Integration Points

**Decision**: Encryption/decryption happens in the DTO mappers (`core:firestore/mapper/`), injecting `FieldCipher` dependency.

**Rationale**: Mappers are the natural boundary between Room entities (always plaintext) and Firestore DTOs (encrypted). This keeps encryption transparent to both SyncManager and PullSyncUseCase — they continue working with mappers as before, just with an additional dependency.

**Data flow**:
- **Push**: `Entity` → `mapper.toDto(entity, fieldCipher)` → encrypted DTO → `FirestoreDataSource.push*(dto)`
- **Pull**: `FirestoreDataSource.pull*()` → DTO (may be encrypted) → `mapper.toEntity(dto, fieldCipher)` → plaintext Entity → Room

**Key insight**: Mapper extension functions must become class methods or accept `FieldCipher` parameter to support encryption. This is the main structural change.
