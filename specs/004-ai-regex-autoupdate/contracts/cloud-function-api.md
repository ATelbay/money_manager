# Cloud Function: Auto-Promote Parser Candidates

## Function: `onParserCandidateUpdated`

**Trigger**: Firestore `onDocumentUpdated("parser_candidates/{candidateId}")`

**Behavior**:
1. Read updated document's `successCount` and `status`
2. If `status != "candidate"` → exit (already processed)
3. If `successCount < threshold` → exit (not ready)
4. Read current Remote Config `parser_configs` JSON
5. Deserialize to `ParserConfigList`
6. Check if `bankId + transactionPattern` already exists in the list → if yes, mark as `rejected`, exit
7. Deserialize `parserConfigJson` from candidate document
8. Append to `ParserConfigList.banks`
9. Publish updated JSON to Remote Config `parser_configs` key
10. Update candidate document: `status = "promoted"`
11. Log promotion event

**Configuration**:
- Promotion threshold: read from Remote Config key `promotion_threshold` (default: 3)
- Region: same as Firestore (e.g., `us-central1`)

**Error Handling**:
- Remote Config update failure → log error, leave candidate as `candidate` (will retry on next update)
- Conflict detection failure → log warning, skip promotion

## Function: `onParserCandidateCreated`

**Trigger**: Firestore `onDocumentCreated("parser_candidates/{candidateId}")`

**Behavior** (optional, for immediate threshold check):
1. Same logic as `onParserCandidateUpdated` but for new documents with `successCount >= threshold`
2. This handles edge cases where the threshold is set to 1

## Directory Structure

```
functions/
├── package.json
├── tsconfig.json
├── src/
│   ├── index.ts              # Function exports
│   └── promote-candidate.ts  # Promotion logic
└── .eslintrc.js
```

## Dependencies

- `firebase-admin` — Firestore reads, Remote Config Admin API
- `firebase-functions` (v2) — Firestore triggers
