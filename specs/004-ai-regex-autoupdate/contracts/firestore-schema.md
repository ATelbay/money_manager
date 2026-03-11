# Firestore Schema: parser_candidates

## Collection: `parser_candidates`

Global collection (not nested under `users/{userId}`).

### Document Structure

```json
{
  "bankId": "halyk_bank",
  "transactionPattern": "(?<date>\\d{2}\\.\\d{2}\\.\\d{4})\\s+(?<sign>[+-]?)(?<amount>[\\d\\s]+,\\d{2})\\s+(?<operation>.+?)\\s+(?<details>.+)",
  "parserConfigJson": "{\"bank_id\":\"halyk_bank\",\"bank_markers\":[\"Халык\",\"Halyk\"],\"transaction_pattern\":\"...\",\"date_format\":\"dd.MM.yyyy\",\"operation_type_map\":{},\"amount_format\":\"space_comma\",\"use_named_groups\":true}",
  "anonymizedSample": "01.01.2026 -15000,00 KZT Покупка MERCHANT_1\n02.01.2026 +50000,00 KZT Зачисление MERCHANT_2",
  "userIdHash": "a3f2b8c9e1d4567890abcdef12345678abcdef1234567890abcdef1234567890",
  "successCount": 1,
  "status": "candidate",
  "createdAt": 1741654800000,
  "updatedAt": 1741654800000
}
```

### Security Rules

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /parser_candidates/{candidateId} {
      // Any authenticated user can create a new candidate
      allow create: if request.auth != null
                    && request.resource.data.keys().hasAll(['bankId', 'transactionPattern', 'parserConfigJson', 'anonymizedSample', 'userIdHash', 'successCount', 'status', 'createdAt', 'updatedAt'])
                    && request.resource.data.successCount == 1
                    && request.resource.data.status == 'candidate';

      // Any authenticated user can increment successCount
      allow update: if request.auth != null
                    && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['successCount', 'updatedAt'])
                    && request.resource.data.successCount == resource.data.successCount + 1;

      // Any authenticated user can read (for matching bankId + transactionPattern)
      allow read: if request.auth != null;

      // Only Cloud Function (admin) can change status
      // (handled by Admin SDK, bypasses rules)
    }
  }
}
```

### Queries

**Find existing candidate for dedup** (client-side):
```
parser_candidates
  WHERE bankId == {bankId}
  AND transactionPattern == {transactionPattern}
  AND status == "candidate"
  LIMIT 1
```

**Find candidates at threshold** (Cloud Function):
```
parser_candidates
  WHERE successCount >= {threshold}
  AND status == "candidate"
```

### Indexes

Composite index required:
- `bankId` (ASC) + `transactionPattern` (ASC) + `status` (ASC)
