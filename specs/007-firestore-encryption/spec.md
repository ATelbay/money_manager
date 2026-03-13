# Feature Specification: Client-Side Firestore Encryption

**Feature Branch**: `007-firestore-encryption`
**Created**: 2026-03-13
**Status**: Draft
**Input**: User description: "Privacy-first client-side encryption for financial data stored in Firestore"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Sensitive Data Invisible in Firebase Console (Priority: P1)

As a user of Money Manager, I want my financial data (transaction amounts, notes, account names, balances, category names) to be encrypted before it leaves my device, so that even the app developer cannot see my spending habits when viewing Firebase Console.

**Why this priority**: This is the core privacy guarantee — the entire reason for this feature. Without it, user financial data is visible to anyone with Firebase Console access.

**Independent Test**: Log into Firebase Console after syncing data and verify that sensitive fields (amounts, names, balances, notes) are unreadable ciphertext while metadata (timestamps, IDs, deletion flags) remains queryable.

**Acceptance Scenarios**:

1. **Given** a user creates a transaction with amount=50000 and note="Salary", **When** the transaction syncs to Firestore, **Then** the `amount` and `note` fields in the Firestore document contain Base64-encoded ciphertext, not plaintext values.
2. **Given** a user creates an account named "Kaspi Gold" with balance=150000, **When** the account syncs to Firestore, **Then** the `name` and `balance` fields are encrypted ciphertext in the Firestore document.
3. **Given** a user creates a category "Groceries" with a specific icon and color, **When** the category syncs to Firestore, **Then** `name`, `icon`, and `color` fields are encrypted in the Firestore document.
4. **Given** encrypted data in Firestore, **When** viewing via Firebase Console, **Then** sensitive fields appear as unreadable Base64 strings while `remoteId`, `updatedAt`, `createdAt`, `isDeleted`, `type`, `currency`, `categoryRemoteId`, `accountRemoteId`, and `date` remain in plaintext.

---

### User Story 2 - Cross-Device Decryption (Priority: P1)

As a user who signs in with the same Google account on a second device, I want all my encrypted financial data to be automatically decrypted and readable on the new device, so that cloud sync works seamlessly despite encryption.

**Why this priority**: Encryption without cross-device decryption breaks the sync feature entirely, making it useless. This is equally critical as encryption itself.

**Independent Test**: Sign in on Device A, create transactions/accounts. Sign in on Device B with the same Google account, verify all data appears correctly decrypted.

**Acceptance Scenarios**:

1. **Given** a user has synced encrypted data from Device A, **When** they sign in on Device B with the same Google account, **Then** all transactions, accounts, and categories are pulled and decrypted correctly with original values intact.
2. **Given** a user creates data on Device B after initial sync, **When** the data syncs to Firestore, **Then** it is encrypted with the same key and can be read back on Device A.

---

### User Story 3 - Legacy Data Migration (Priority: P2)

As an existing user who already has unencrypted data in Firestore, I want the app to seamlessly read my old unencrypted data and re-encrypt it, so that I don't lose any data during the transition to encryption.

**Why this priority**: Existing users must not lose data. The migration must be transparent — no manual action required.

**Independent Test**: Pre-populate Firestore with unencrypted documents, install the updated app, sign in, and verify all data loads correctly and gets re-encrypted on next push.

**Acceptance Scenarios**:

1. **Given** a user has existing unencrypted transactions in Firestore, **When** the app pulls data after the encryption update, **Then** the app detects legacy plaintext fields and reads them without error.
2. **Given** legacy unencrypted data has been pulled, **When** the data is next modified or the app performs a full sync, **Then** the data is pushed back encrypted.
3. **Given** a mix of encrypted and unencrypted documents in the same collection, **When** the app pulls all documents, **Then** each document is handled correctly based on whether its fields are encrypted or plaintext.

---

### Edge Cases

- What happens when a user signs out and signs in with a different Google account? Each account derives its own encryption key — data encrypted by one account cannot be decrypted by another. This is the expected privacy behavior.
- What happens if the encryption key derivation input (UID) changes? UIDs are stable per Google account in Firebase Auth — they do not change. If Firebase were to reassign a UID (extremely unlikely), the user's data would become unreadable.
- What happens with default categories that use deterministic remote IDs (`default:{name}:{type}`)? Default category names/icons/colors are still encrypted per-user. The deterministic `remoteId` pattern remains in plaintext for cross-device category matching.
- What happens to `ParserCandidateDto` data? Parser candidates are stored in a root-level shared collection (not user-scoped) and already use `userIdHash` for privacy. They are excluded from encryption.
- What happens if decryption fails (corrupted data, wrong key)? The app should log the error via Timber and skip the corrupted document rather than crashing. User-facing notification is out of scope for MVP — silent skip with logging is sufficient.
- What happens with nullable fields (e.g., `note`, `uniqueHash` in TransactionDto)? Null fields remain null — only non-null values are encrypted.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST encrypt the following fields before pushing to Firestore: `TransactionDto.amount`, `TransactionDto.note`, `TransactionDto.uniqueHash`, `AccountDto.name`, `AccountDto.balance`, `CategoryDto.name`, `CategoryDto.icon`, `CategoryDto.color`.
- **FR-002**: System MUST keep the following fields in plaintext for query and sync purposes: all `remoteId` fields, `updatedAt`, `createdAt`, `isDeleted`, `TransactionDto.type`, `TransactionDto.categoryRemoteId`, `TransactionDto.accountRemoteId`, `TransactionDto.date`, `AccountDto.currency`.
- **FR-003**: System MUST derive the encryption key from the user's Firebase UID, ensuring that the same Google account produces the same key on any device.
- **FR-004**: System MUST use AES-256-GCM for encryption with a unique initialization vector (IV) per encrypted field to prevent pattern analysis.
- **FR-005**: System MUST prepend the IV to each encrypted field's ciphertext so that decryption is self-contained per field.
- **FR-006**: System MUST include an `encryptionVersion` field on every DTO (0=plaintext, 1=AES-GCM). Missing field is treated as 0. Legacy unencrypted documents are read as plaintext based on this version indicator.
- **FR-007**: System MUST NOT modify local Room database entities — encryption and decryption happen exclusively at the Firestore sync boundary.
- **FR-008**: System MUST handle null fields gracefully — null values are not encrypted and remain null.
- **FR-009**: System MUST cache the derived encryption key in memory only (singleton). The key is re-derived from the UID on process restart.
- **FR-010**: System MUST clear the in-memory encryption key when the user signs out.
- **FR-011**: System MUST NOT encrypt `ParserCandidateDto` documents as they are stored in a shared collection with existing privacy measures.
- **FR-012**: System MUST gracefully handle decryption failures (corrupted data, wrong key) by skipping the affected document and logging the error, without crashing.

### Key Entities

- **Encryption Key**: Derived from Firebase UID via key derivation function. One key per user account. Cached in-memory only (singleton), cleared on sign-out or process death.
- **Encrypted Field**: A sensitive data field converted to Base64-encoded AES-256-GCM ciphertext before Firestore storage. Decrypted back to original type on read.
- **Initialization Vector (IV)**: A unique random value per encrypted field, prepended to that field's ciphertext. Ensures identical plaintext produces different ciphertext each time.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of sensitive financial fields (amounts, names, balances, notes, icons, colors) are unreadable in Firebase Console after sync.
- **SC-002**: Users on a second device see all their data correctly within the existing sync time (no noticeable performance degradation from encryption/decryption).
- **SC-003**: Existing users with unencrypted Firestore data experience zero data loss after updating to the encrypted version.
- **SC-004**: The encryption/decryption process adds no more than 100ms overhead per sync operation (batch of up to 500 documents).
- **SC-005**: Sign-out fully clears encryption key material from the device — no sensitive key data persists after logout.

## Clarifications

### Session 2026-03-13

- Q: Legacy detection — `encryptionVersion` field vs heuristic Base64 parsing? → A: `encryptionVersion` field on every DTO (0=plaintext, 1=AES-GCM). Missing field treated as 0.
- Q: IV granularity — per-document or per-field? → A: Unique IV per field. Each encrypted field gets its own random IV prepended to its ciphertext.
- Q: Key caching mechanism — in-memory only or Android Keystore? → A: In-memory only. Key re-derived from UID on process restart (HKDF is cheap).

## Assumptions

- Firebase UID is stable and unique per Google account — it does not change over the lifetime of the account.
- The key derivation uses a fixed application-specific salt (hardcoded or from app config) combined with the UID, so the same UID always produces the same key.
- AES-256-GCM is sufficient for the privacy requirements — no need for additional encryption layers.
- The volume of data per user is small enough (typical personal finance app) that encrypting/decrypting all fields during sync does not cause performance issues.
- Existing unencrypted data is distinguished from encrypted data via the `encryptionVersion` field: missing or 0 means plaintext, 1 means AES-GCM encrypted.
