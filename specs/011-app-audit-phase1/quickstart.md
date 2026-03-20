# Quickstart: App Audit Phase 1

## Branch

```bash
git checkout 011-app-audit-phase1
```

## Build & Test

```bash
./gradlew assembleDebug    # Full build
./gradlew test             # Unit tests
./gradlew lint             # Lint checks
```

## Changes Overview

This feature is a **bug-fix and quality audit** — no new screens or user flows. All changes are to existing files, except:

### New Files
| File | Purpose |
|------|---------|
| `core/database/.../migration/Migration_3_4.kt` | Room migration: add indexes |
| `domain/sync/build.gradle.kts` | New module |
| `domain/sync/.../SyncRepository.kt` | Domain interface for sync |
| `data/sync/.../SyncRepositoryImpl.kt` | Implementation wrapping SyncManager |
| `data/sync/.../di/SyncModule.kt` | Hilt binding |

### Modified Files (by area)

**S1 — Backup rules** (2 XML files):
- `app/src/main/res/xml/data_extraction_rules.xml`
- `app/src/main/res/xml/backup_rules.xml`

**A2 — Save button error handling** (3 ViewModels):
- `presentation/accounts/.../AccountEditViewModel.kt`
- `presentation/categories/.../CategoryEditViewModel.kt`
- `presentation/transactions/.../TransactionEditViewModel.kt`

**L1+L2 — Localization** (1 strings file + 8 consumers):
- `core/ui/.../theme/AppStrings.kt` (add ~22 new keys)
- `presentation/accounts/.../AccountEditViewModel.kt`
- `presentation/categories/.../CategoryEditViewModel.kt`
- `presentation/transactions/.../TransactionEditViewModel.kt`
- `presentation/import/.../ImportViewModel.kt`
- `presentation/import/.../ImportPreview.kt`
- `presentation/auth/.../SignInViewModel.kt`
- `presentation/statistics/.../CategoryTransactionsViewModel.kt`
- `presentation/settings/.../SettingsViewModel.kt`

**D1 — Database indexes** (4 files):
- `core/database/.../entity/AccountEntity.kt`
- `core/database/.../entity/CategoryEntity.kt`
- `core/database/.../entity/TransactionEntity.kt`
- `core/database/.../MoneyManagerDatabase.kt` (version bump 3→4)
- `core/database/.../di/DatabaseModule.kt` (add migration)
- `core/database/.../migration/Migration_3_4.kt` (new)

**A1 — Sync architecture** (5+ files):
- `core/model/.../SyncStatus.kt` (moved from data:sync)
- `domain/sync/` (new module)
- `data/sync/.../SyncRepositoryImpl.kt` (new)
- `data/sync/.../SyncStatus.kt` (delete — moved to core:model)
- `presentation/settings/build.gradle.kts` (remove data:sync dep)
- `presentation/settings/.../SettingsViewModel.kt` (use SyncRepository)
- `settings.gradle.kts` (register domain:sync)

## Verification

```bash
# Build check
./gradlew assembleDebug

# Unit tests
./gradlew test

# Verify no presentation→data dependency
grep -r "data.sync" presentation/settings/build.gradle.kts
# Should return empty

# Verify backup rules (on device)
adb shell bmgr backupnow com.atelbay.money_manager
```
