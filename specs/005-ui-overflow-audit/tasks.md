# Tasks: UI Overflow & Layout Audit

**Input**: Design documents from `/specs/005-ui-overflow-audit/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md

**Tests**: Not requested — no test tasks generated. Visual verification via @Preview with extreme values.

**Organization**: Tasks grouped by user story. US1–US3 are P1 (main screen), US4–US6/US8 are P2 (secondary screens), US7 is P3 (settings/auth).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story (US1–US8)
- Exact file paths included in all tasks

---

## Phase 1: Setup

**Purpose**: No new modules or dependencies. Verify `TextAutoSize` API availability.

- [ ] T001 Verify `TextAutoSize` import resolves in project — create a throwaway @Preview in `core/ui/src/main/java/com/atelbay/money_manager/core/ui/components/BalanceCard.kt` using `autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 34.sp)` on a `Text`, run Compose preview to confirm it compiles

**Checkpoint**: TextAutoSize API confirmed available — proceed to implementation.

---

## Phase 2: Foundational

**Purpose**: No blocking prerequisites — all tasks are per-component and can proceed directly after Phase 1.

*(No foundational tasks — this is a UI-only audit with no shared infrastructure changes.)*

**Checkpoint**: Foundation ready — user story implementation can begin.

---

## Phase 3: User Story 1 — Large Balance Display (Priority: P1) 🎯 MVP

**Goal**: BalanceCard displays balances up to 13 digits without overflow, using autoSize to shrink font when needed.

**Independent Test**: Create account with balance 9,999,999,999,999.99 → open transaction list → balance fully visible in BalanceCard.

### Implementation for User Story 1

- [ ] T002 [US1] Add `autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = 34.sp, stepSize = 1.sp)` to balance `Text` in `core/ui/src/main/java/com/atelbay/money_manager/core/ui/components/BalanceCard.kt`. Ensure `maxLines = 1` is set. Verify parent provides bounded width constraints.
- [ ] T003 [US1] Add/update @Preview in `BalanceCard.kt` with extreme values: balance=0.00, balance=9999999999999.99, balance=−9999999999999.99, currency="KZT". Verify all render correctly in preview.

**Checkpoint**: BalanceCard handles 13-digit balances — main screen balance is fixed.

---

## Phase 4: User Story 2 — Income/Expense Summary Card (Priority: P1)

**Goal**: IncomeExpenseCard displays all 4 values (income, expense, balance, savings %) without overlap at max amounts.

**Independent Test**: Add transactions totaling 1,500,000,000+ income and 1,200,000,000+ expense → open transaction list → all values in IncomeExpenseCard fully visible.

### Implementation for User Story 2

- [ ] T004 [US2] Add `autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = 16.sp, stepSize = 1.sp)` to income amount Text, expense amount Text, and net balance Text in `core/ui/src/main/java/com/atelbay/money_manager/core/ui/components/IncomeExpenseCard.kt`. Set `maxLines = 1` on each.
- [ ] T005 [US2] Verify savings percentage Text has `maxLines = 1` and does not overflow when showing "99.9%" alongside large amounts in `IncomeExpenseCard.kt`.
- [ ] T006 [US2] Add/update @Preview in `IncomeExpenseCard.kt` with extreme values: income=9999999999999.99, expense=9999999999999.99, currency="KZT".

**Checkpoint**: IncomeExpenseCard handles 13-digit amounts — main screen summary is fixed.

---

## Phase 5: User Story 3 — Transaction List Item (Priority: P1)

**Goal**: TransactionListItem shows full amount (via autoSize) and truncates description (via ellipsis) when needed.

**Independent Test**: Create transaction with 50+ char description and amount 999,999,999,999.99 → verify in list.

### Implementation for User Story 3

- [ ] T007 [US3] In `core/ui/src/main/java/com/atelbay/money_manager/core/ui/components/TransactionListItem.kt`, replace `TextOverflow.Ellipsis` on the primary amount Text with `autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = 16.sp, stepSize = 1.sp)`. Keep `maxLines = 1`.
- [ ] T008 [US3] In `TransactionListItem.kt`, replace `TextOverflow.Ellipsis` on the secondary amount Text (converted currency) with same `autoSize` parameters. Keep `maxLines = 1`.
- [ ] T009 [US3] Verify description Text in `TransactionListItem.kt` has `maxLines = 1`, `overflow = TextOverflow.Ellipsis`, and `Modifier.weight(1f)` so it yields space to the amount column.
- [ ] T010 [US3] Add/update @Preview in `TransactionListItem.kt` with: description="Оплата за долгосрочную аренду офисного помещения в бизнес-центре", amount=9999999999999.99, secondaryAmount=9999999999999.99, currency="KZT".

**Checkpoint**: Transaction list items handle extreme amounts — P1 stories complete, main screen fully fixed.

---

## Phase 6: User Story 4 — Account List Overflow (Priority: P2)

**Goal**: AccountCard and AccountListScreen total balance handle long names (ellipsis) and large balances (autoSize).

**Independent Test**: Create account with 40+ char name and balance 9,999,999,999,999.99 → open account list.

### Implementation for User Story 4

- [ ] T011 [P] [US4] Add `autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 28.sp, stepSize = 1.sp)` to balance Text in `core/ui/src/main/java/com/atelbay/money_manager/core/ui/components/AccountCard.kt`. Add `maxLines = 1, overflow = TextOverflow.Ellipsis` to account name Text.
- [ ] T012 [P] [US4] In `presentation/accounts/src/main/java/com/atelbay/money_manager/presentation/accounts/ui/list/AccountListScreen.kt`, find the total balance display Text and add `autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 20.sp, stepSize = 1.sp)` with `maxLines = 1`.
- [ ] T013 [US4] Add/update @Preview in `AccountCard.kt` with: name="Мой долгосрочный сберегательный депозит в Каспи Банке", balance=9999999999999.99, currency="KZT".

**Checkpoint**: Account list handles overflow — accounts screen fixed.

---

## Phase 7: User Story 5 — Statistics Screen Overflow (Priority: P2)

**Goal**: SummaryStatCard and category breakdown rows handle 13-digit amounts.

**Independent Test**: With transactions totaling 2,500,000,000+ → open statistics → all cards and category rows readable.

### Implementation for User Story 5

- [ ] T014 [P] [US5] Add `autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = 16.sp, stepSize = 1.sp)` to value Text in `core/ui/src/main/java/com/atelbay/money_manager/core/ui/components/SummaryStatCard.kt`. Set `maxLines = 1`.
- [ ] T015 [P] [US5] In `presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/StatisticsScreen.kt`, find the donut chart center total Text and replace ellipsis with `autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 20.sp, stepSize = 1.sp)`.
- [ ] T016 [US5] In `StatisticsScreen.kt`, find the category breakdown row amount Text and add `autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = 16.sp, stepSize = 1.sp)`. Ensure category name Text has `maxLines = 1, overflow = TextOverflow.Ellipsis, Modifier.weight(1f)`.
- [ ] T017 [US5] Add/update @Preview for SummaryStatCard with value=9999999999999.99, currency="KZT".

**Checkpoint**: Statistics screen handles overflow — all stat cards and category rows fixed.

---

## Phase 8: User Story 6 — Import Preview Overflow (Priority: P2)

**Goal**: ParsedTransactionItem shows amounts and descriptions correctly with extreme values.

**Independent Test**: Import statement with 60+ char descriptions and amounts 500,000,000+ → preview all readable.

### Implementation for User Story 6

- [ ] T018 [P] [US6] In `presentation/import/src/main/java/com/atelbay/money_manager/presentation/importstatement/ui/components/ParsedTransactionItem.kt`, add `autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = 16.sp, stepSize = 1.sp)` with `maxLines = 1` to amount Text.
- [ ] T019 [US6] In `ParsedTransactionItem.kt`, ensure description Text has `maxLines = 1, overflow = TextOverflow.Ellipsis, Modifier.weight(1f)`.

**Checkpoint**: Import preview handles overflow — import screen fixed.

---

## Phase 9: User Story 8 — Edit Screens Overflow (Priority: P2)

**Goal**: Hero amount input and name fields handle extreme values without breaking form layout.

**Independent Test**: Open create transaction → enter 9,999,999,999,999.99 → verify input + save button visible.

### Implementation for User Story 8

- [ ] T020 [P] [US8] In `core/ui/src/main/java/com/atelbay/money_manager/core/ui/components/MoneyManagerTextField.kt`, find the `MoneyManagerAmountField` composable and add `autoSize = TextAutoSize.StepBased(minFontSize = 16.sp, maxFontSize = 30.sp, stepSize = 1.sp)` to the amount display Text (or adapt the BasicTextField fontSize dynamically). Ensure `singleLine = true`.
- [ ] T021 [P] [US8] Verify `presentation/transactions/src/main/java/com/atelbay/money_manager/presentation/transactions/ui/edit/TransactionEditScreen.kt` — with amount 9,999,999,999,999.99 the hero input, type chips, category picker, date picker, and save button all remain visible and accessible. Fix any overflow found.
- [ ] T022 [P] [US8] Verify `presentation/accounts/src/main/java/com/atelbay/money_manager/presentation/accounts/ui/edit/AccountEditScreen.kt` — with name 50+ chars, the text field scrolls internally and save button remains visible. Fix any overflow found.
- [ ] T023 [P] [US8] Verify `presentation/categories/src/main/java/com/atelbay/money_manager/presentation/categories/ui/edit/CategoryEditScreen.kt` — with name 30+ chars, the text field and all pickers remain visible. Fix any overflow found.
- [ ] T024 [US8] In `core/ui/src/main/java/com/atelbay/money_manager/core/ui/components/CategoryPicker.kt`, add `maxLines = 1, overflow = TextOverflow.Ellipsis` to category name labels in the 3-column grid to prevent layout breaking with long names.

**Checkpoint**: Edit screens handle extreme input — all P2 screens fixed.

---

## Phase 10: User Story 7 — Settings & Auth Screen (Priority: P3)

**Goal**: Settings rows and auth screen handle long email/name without overflow.

**Independent Test**: Sign in with long email (30+ chars) → open settings → email/name readable.

### Implementation for User Story 7

- [ ] T025 [P] [US7] In `presentation/settings/src/main/java/com/atelbay/money_manager/presentation/settings/ui/SettingsScreen.kt`, verify account section (email, display name, sync status) has `maxLines = 1, overflow = TextOverflow.Ellipsis` on all potentially long text. Fix any missing overflow handling.
- [ ] T026 [P] [US7] In `presentation/auth/src/main/java/com/atelbay/money_manager/presentation/auth/ui/SignInScreen.kt`, verify existing `TextOverflow.Ellipsis` on display name and email is working correctly. No changes expected — confirm only.

**Checkpoint**: Settings and auth screens handle overflow — P3 complete.

---

## Phase 11: Polish & Cross-Cutting Concerns

**Purpose**: Final verification and cleanup across all modified components.

- [ ] T027 Run `./gradlew assembleDebug` to verify all changes compile without errors
- [ ] T028 Run `./gradlew lint` and fix any new lint warnings introduced by the changes
- [ ] T029 Visual review of all @Preview composables added/updated in this audit — verify extreme values render correctly at 360dp width simulation

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — start immediately
- **Phase 2 (Foundational)**: N/A — no blocking prerequisites
- **Phases 3–5 (P1 stories)**: Can start after Phase 1. Recommended sequential: US1 → US2 → US3 (shared `core:ui` files)
- **Phases 6–9 (P2 stories)**: Can start after Phase 1. All independent of each other — run in parallel.
- **Phase 10 (P3)**: Can start after Phase 1. Independent — run in parallel with P2.
- **Phase 11 (Polish)**: After all implementation phases complete.

### User Story Dependencies

- **US1 (BalanceCard)**: Independent. Start first.
- **US2 (IncomeExpenseCard)**: Independent. Can parallel with US1 (different file).
- **US3 (TransactionListItem)**: Independent. Can parallel with US1/US2 (different file).
- **US4 (AccountCard + AccountListScreen)**: Independent. Parallel with all others.
- **US5 (SummaryStatCard + StatisticsScreen)**: Independent. Parallel with all others.
- **US6 (ParsedTransactionItem)**: Independent. Parallel with all others.
- **US8 (Edit screens)**: Depends on T020 (MoneyManagerAmountField) for TransactionEdit verification.
- **US7 (Settings/Auth)**: Independent. Verification only.

### Parallel Opportunities

All user stories modify **different files** and can execute in parallel:

```
Parallel Group A (core:ui components — different files):
  T002-T003 (US1: BalanceCard)
  T004-T006 (US2: IncomeExpenseCard)
  T007-T010 (US3: TransactionListItem)
  T011,T013 (US4: AccountCard)
  T014,T017 (US4: SummaryStatCard)

Parallel Group B (presentation screens — different modules):
  T012 (US4: AccountListScreen)
  T015-T016 (US5: StatisticsScreen)
  T018-T019 (US6: ParsedTransactionItem)
  T020-T024 (US8: Edit screens)
  T025-T026 (US7: Settings/Auth)
```

---

## Implementation Strategy

### MVP First (P1 Only: US1 + US2 + US3)

1. T001: Verify TextAutoSize API
2. T002-T003: Fix BalanceCard
3. T004-T006: Fix IncomeExpenseCard
4. T007-T010: Fix TransactionListItem
5. **STOP and VALIDATE**: Main screen fully handles overflow
6. Commit as standalone PR if needed

### Full Delivery

1. MVP (above)
2. T011-T017: Fix AccountCard, AccountListScreen, SummaryStatCard, StatisticsScreen (all parallelizable)
3. T018-T024: Fix ImportPreview, Edit screens, CategoryPicker (all parallelizable)
4. T025-T026: Verify Settings/Auth
5. T027-T029: Build, lint, visual review

---

## Notes

- All tasks modify UI layer only — no ViewModel, domain, or data changes
- `TextAutoSize.StepBased()` requires parent with bounded width — verify `fillMaxWidth()` or explicit constraints
- `autoSize` and `TextOverflow.Ellipsis` are mutually exclusive on same Text — use autoSize for amounts, ellipsis for descriptive text
- Min font 11-12sp per component (see research.md R-003 table for exact values)
- Commit after each user story phase for easy rollback
