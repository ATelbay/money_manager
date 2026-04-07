# TODO: Deferred from 017-bugfixes-and-sync

## `pendingSync` Dirty Flag — Full Sync Reliability

**Context**: Currently `pushAllPending()` only catches entities with `remoteId IS NULL`. Entities that have a `remoteId` but whose last push failed (e.g., network error after Firestore returned a remoteId) are silently lost until the next full re-sync.

**What to do**:
- Add a `pendingSync: Boolean` column (default `false`) to all synced entity tables: accounts, categories, transactions, budgets, recurring_transactions
- Room migration: additive column with `DEFAULT 0`
- Set `pendingSync = true` on local create/update/delete
- Clear `pendingSync = false` after successful Firestore push
- Update `pushAllPending()` to also query `WHERE pendingSync = 1` (in addition to `remoteId IS NULL`)
- Add retry/backoff logic for failed pushes

**Why deferred**: Cross-cutting migration across 5 entity tables + push-retry logic. Better to ship budget/recurring sync first with the existing pattern, prove it stable, then retrofit all entities with `pendingSync` in a dedicated spec.

**Priority**: Medium — affects reliability of incremental sync but full re-sync on login covers the gap.
