# Data Model: Client-Side Firestore Encryption

**Branch**: `007-firestore-encryption` | **Date**: 2026-03-13

## New Entity: FieldCipher

**Module**: `core:crypto`
**Purpose**: Encrypts/decrypts individual field values at the Firestore boundary.

| Attribute | Type | Description |
|-----------|------|-------------|
| aesKey | ByteArray (in-memory) | 32-byte AES-256 key derived from Firebase UID via HKDF |

**Operations**:
- `encrypt(plaintext: String): String` — returns Base64-encoded `IV || ciphertext || tag`
- `decrypt(ciphertext: String): String` — decodes Base64, extracts IV, decrypts, returns plaintext
- `encryptDouble(value: Double): String` — converts to string, encrypts
- `decryptDouble(ciphertext: String): Double` — decrypts, parses to Double
- `encryptLong(value: Long): String` — converts to string, encrypts
- `decryptLong(ciphertext: String): Long` — decrypts, parses to Long

**Lifecycle**:
- Created when user signs in (UID available)
- Cached in-memory as singleton (Hilt `@Singleton`)
- Cleared on sign-out (reference set to null / re-derive on next sign-in)
- Re-derived on process restart from current UID (HKDF is fast)

## Modified Entity: TransactionDto

**Module**: `core:firestore`

| Field | Old Type | New Type | Encrypted | Notes |
|-------|----------|----------|-----------|-------|
| remoteId | String | String | No | `@DocumentId` |
| amount | Double | String | **Yes** | Base64 ciphertext of Double value |
| type | String | String | No | Plaintext for queries |
| categoryRemoteId | String | String | No | Foreign key, plaintext |
| accountRemoteId | String | String | No | Foreign key, plaintext |
| note | String? | String? | **Yes** | Null remains null |
| date | Long | Long | No | Plaintext for queries |
| createdAt | Long | Long | No | Plaintext for sync |
| updatedAt | Long | Long | No | Plaintext for sync |
| isDeleted | Boolean | Boolean | No | Plaintext for sync |
| uniqueHash | String? | String? | **Yes** | Null remains null |
| **encryptionVersion** | — | Int | No | **New field**: 0=plaintext, 1=AES-GCM |

## Modified Entity: AccountDto

**Module**: `core:firestore`

| Field | Old Type | New Type | Encrypted | Notes |
|-------|----------|----------|-----------|-------|
| remoteId | String | String | No | `@DocumentId` |
| name | String | String | **Yes** | Base64 ciphertext |
| currency | String | String | No | Plaintext for display |
| balance | Double | String | **Yes** | Base64 ciphertext of Double value |
| createdAt | Long | Long | No | Plaintext for sync |
| updatedAt | Long | Long | No | Plaintext for sync |
| isDeleted | Boolean | Boolean | No | Plaintext for sync |
| **encryptionVersion** | — | Int | No | **New field**: 0=plaintext, 1=AES-GCM |

## Modified Entity: CategoryDto

**Module**: `core:firestore`

| Field | Old Type | New Type | Encrypted | Notes |
|-------|----------|----------|-----------|-------|
| remoteId | String | String | No | `@DocumentId` |
| name | String | String | **Yes** | Base64 ciphertext |
| icon | String | String | **Yes** | Base64 ciphertext |
| color | Long | String | **Yes** | Base64 ciphertext of Long value |
| type | String | String | No | Plaintext for queries |
| updatedAt | Long | Long | No | Plaintext for sync |
| isDeleted | Boolean | Boolean | No | Plaintext for sync |
| **encryptionVersion** | — | Int | No | **New field**: 0=plaintext, 1=AES-GCM |

## Unchanged Entity: ParserCandidateDto

No changes. Stored in root `parser_candidates` collection, not user-scoped. Already uses `userIdHash` for privacy.

## Unchanged: All Room Entities

No changes to `TransactionEntity`, `AccountEntity`, `CategoryEntity`. Encryption is transparent at the Firestore boundary only.

## Relationships

```
Firebase UID ──HKDF──→ FieldCipher (1:1 per user session)
                            │
                            ├── encrypts → TransactionDto.{amount, note, uniqueHash}
                            ├── encrypts → AccountDto.{name, balance}
                            └── encrypts → CategoryDto.{name, icon, color}
```

## Migration: Legacy (encryptionVersion=0) → Encrypted (encryptionVersion=1)

**Pull path (Firestore → App)**:
1. Read `encryptionVersion` from document. Missing = 0.
2. If 0: read fields as original types (Double/Long for numeric), convert to domain values directly.
3. If 1: read all encrypted fields as String, decrypt via FieldCipher, parse to domain types.

**Push path (App → Firestore)**:
1. Always encrypt sensitive fields.
2. Always set `encryptionVersion = 1`.
3. Existing plaintext documents are overwritten with encrypted versions on next sync.

**Firestore deserialization change**:
- Pull methods switch from `toObject(Dto::class.java)` to `document.data` (Map<String, Any?>) for DTOs with encrypted fields.
- Manual DTO construction handles type polymorphism (legacy Double vs encrypted String for `amount`/`balance`/`color`).
