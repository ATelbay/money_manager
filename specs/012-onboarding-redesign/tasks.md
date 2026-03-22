# Tasks: Onboarding Screens Redesign

**Input**: Design documents from `/specs/012-onboarding-redesign/`
**Prerequisites**: plan.md (required), spec.md (required), research.md

**Tests**: Not requested — no test tasks included.

**Organization**: US2 (Outfit font) is a foundational prerequisite for US1 (visual redesign). US1 tasks are ordered: colors → page content → buttons → indicator → layout → preview.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Include exact file paths in descriptions

## Design Reference

- **Pencil file**: `~/Documents/pencil/money_manager_ds/money_manager_screens.pen`
- **Page 1**: node `vq5yF` | **Page 2**: node `FuS5O` | **Page 3**: node `LXNau`
- Use `get_screenshot(nodeId)` to verify visually during implementation.

---

## Phase 1: Setup

**Purpose**: Download and add Outfit font resource files

- [ ] T001 [US2] Download Outfit font from Google Fonts and add `outfit_regular.ttf` (W400), `outfit_medium.ttf` (W500), `outfit_semibold.ttf` (W600) to `core/ui/src/main/res/font/`

---

## Phase 2: Foundational — Outfit Font Family (US2)

**Purpose**: Define OutfitFontFamily in core:ui so onboarding can use it

**⚠️ CRITICAL**: US1 (visual redesign) cannot begin until this phase is complete

- [ ] T002 [US2] Define `OutfitFontFamily` as a `FontFamily` with 3 `Font` entries (Regular/W400, Medium/W500, SemiBold/W600) in `core/ui/src/main/java/com/atelbay/money_manager/core/ui/theme/Type.kt`. Do NOT modify `MoneyManagerTypography` or `MoneyManagerExtendedTypography`. Verify the app builds after this change.

**Checkpoint**: `OutfitFontFamily` is available for import from `core:ui`. Global typography unchanged. App compiles.

---

## Phase 3: User Story 1 — Redesigned Onboarding UI (Priority: P1) 🎯 MVP

**Goal**: Redesign all 3 onboarding pages to match Pencil design with custom colors, Outfit typography, circular icon containers, custom buttons, and updated spacing.

**Independent Test**: Clear app data → launch → verify all 3 pages render with off-white background, green circular icon containers, Outfit font text, green action button, and gray skip text. Verify swipe, "Далее"/"Начать" switching, and skip hidden on last page.

### Implementation for User Story 1

- [ ] T003 [US1] Define onboarding-local color constants as top-level `private val`s at the top of `presentation/onboarding/src/main/java/com/atelbay/money_manager/presentation/onboarding/ui/OnboardingScreen.kt`: `OnboardingGreen` (#3D8A5A), `OnboardingGreenLight` (#3D8A5A at 7% opacity via `.copy(alpha = 0.07f)`), `OnboardingBackground` (#F5F4F1), `OnboardingTextPrimary` (#1A1918), `OnboardingTextSecondary` (#6D6C6A), `OnboardingSkipColor` (#9C9B99), `OnboardingDotInactive` (#D4D3D0)

- [ ] T004 [US1] Redesign `OnboardingPageContent` composable in `presentation/onboarding/src/main/java/com/atelbay/money_manager/presentation/onboarding/ui/OnboardingScreen.kt`: replace bare 120dp Icon with 64dp Icon inside a 160dp circular `Box` (CircleShape, OnboardingGreenLight background, OnboardingGreen tint). Update title to Outfit SemiBold 26sp, letterSpacing -0.5sp, OnboardingTextPrimary. Update description to Outfit Regular 15sp, lineHeight 22.5sp (1.5×15), OnboardingTextSecondary, max width 300dp. Set spacing: 32dp between icon container and title, 12dp between title and description. Use pencil `get_screenshot("vq5yF")` to verify Page 1 content layout.

- [ ] T005 [US1] Redesign main action button in `OnboardingScreen` composable in `presentation/onboarding/src/main/java/com/atelbay/money_manager/presentation/onboarding/ui/OnboardingScreen.kt`: replace `MoneyManagerButton` with a `Button` using `ButtonDefaults.buttonColors(containerColor = OnboardingGreen, contentColor = Color.White)`, `RoundedCornerShape(14.dp)`, `height(52.dp)`, `fillMaxWidth()`. Button text in Outfit Medium 16sp. Preserve `testTag("onboarding:nextButton")` and "Далее"/"Начать" text switching logic. Remove the `MoneyManagerButton` import.

- [ ] T006 [US1] Redesign skip action in `OnboardingScreen` composable in `presentation/onboarding/src/main/java/com/atelbay/money_manager/presentation/onboarding/ui/OnboardingScreen.kt`: replace `MoneyManagerTextButton` with a plain `Text` composable using `Modifier.clickable(onClick = onSkipClick)`, Outfit Medium 14sp, OnboardingSkipColor. Preserve `testTag("onboarding:skipButton")` and hidden-on-last-page logic. Remove the `MoneyManagerTextButton` import.

- [ ] T007 [US1] Update `PageIndicator` composable in `presentation/onboarding/src/main/java/com/atelbay/money_manager/presentation/onboarding/ui/OnboardingScreen.kt`: change active dot color from `MaterialTheme.colorScheme.primary` to `OnboardingGreen`. Change inactive dot color from `MaterialTheme.colorScheme.outlineVariant` to `OnboardingDotInactive`. Keep 8dp size, 8dp spacing, CircleShape, and animated color transition.

- [ ] T008 [US1] Update `OnboardingScreen` layout in `presentation/onboarding/src/main/java/com/atelbay/money_manager/presentation/onboarding/ui/OnboardingScreen.kt`: set root Column background to `OnboardingBackground` (or apply `.background(OnboardingBackground)` to `fillMaxSize()`). Set horizontal padding 24dp, bottom padding 40dp. Set 32dp spacer between pager and indicator. Set 12dp spacer between button and skip. Preserve `testTag("onboarding:screen")`, `testTag("onboarding:pager")`, and `Spacer(Modifier.weight(1f))` vertical centering. Preserve all HorizontalPager logic and `LaunchedEffect` blocks unchanged.

- [ ] T009 [US1] Update `OnboardingScreenPreview` in `presentation/onboarding/src/main/java/com/atelbay/money_manager/presentation/onboarding/ui/OnboardingScreen.kt`: ensure preview renders with the new design. Clean up any unused imports (e.g., `MoneyManagerButton`, `MoneyManagerTextButton` if fully removed).

**Checkpoint**: All 3 onboarding pages match the Pencil design. All testTags preserved. Pager, button text switching, and skip logic work identically to before.

---

## Phase 4: Polish & Verification

**Purpose**: Build verification and visual comparison with Pencil design

- [ ] T010 [US1] Run `./gradlew assembleDebug` to verify the app builds without errors after all changes
- [ ] T011 [US1] Run `./gradlew :presentation:onboarding:test` to verify existing onboarding unit tests pass with the redesigned UI
- [ ] T012 [US1] Use pencil MCP `get_screenshot` for all 3 design nodes (vq5yF, FuS5O, LXNau) and visually compare with the app running on emulator. Document any discrepancies.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — start immediately
- **Phase 2 (Foundational)**: Depends on Phase 1 (font files must exist before referencing R.font.*)
- **Phase 3 (US1)**: Depends on Phase 2 (OutfitFontFamily must be defined)
- **Phase 4 (Polish)**: Depends on Phase 3 completion

### Within Phase 3 (US1)

- T003 (colors) → no dependencies within phase, but must come first since T004-T008 reference color constants
- T004 (page content), T005 (button), T006 (skip), T007 (indicator) → all depend on T003, but can run in sequence since they modify the same file
- T008 (layout) → depends on T004-T007 being done (same file, needs final structure)
- T009 (preview/cleanup) → last, after all composable changes

### User Story Dependencies

- **US2 (Outfit Font)**: Foundational — must complete before US1
- **US1 (Visual Redesign)**: Depends on US2 — this is the MVP deliverable

### Parallel Opportunities

- T001 and T002 are sequential (same module, T002 references T001 outputs)
- T004, T005, T006, T007 modify the same file so cannot be truly parallelized, but are logically independent composable changes
- T010 and T011 can run in parallel after Phase 3

---

## Parallel Example: Phase 4

```bash
# After Phase 3 is complete, run verification tasks in parallel:
Task: "Run ./gradlew assembleDebug to verify build"
Task: "Visual comparison with Pencil design screenshots"
```

---

## Implementation Strategy

### MVP First (All tasks — small feature)

1. Complete Phase 1: Add font files (T001)
2. Complete Phase 2: Define OutfitFontFamily (T002)
3. Complete Phase 3: Redesign OnboardingScreen (T003-T009)
4. **STOP and VALIDATE**: Build + visual comparison (T010-T011)

### Single-File Workflow

Since T003-T009 all modify `OnboardingScreen.kt`, the recommended approach is:
1. Add color constants (T003)
2. Work through composables top-to-bottom: page content → button → skip → indicator → layout
3. Clean up imports and preview last (T009)
4. This minimizes merge conflicts and keeps the file in a compilable state at each step

---

## Notes

- All T003-T009 tasks modify the same file (`OnboardingScreen.kt`) — execute sequentially
- T002 modifies `Type.kt` — independent of OnboardingScreen changes
- Preserve ALL existing testTags (FR-008): `onboarding:screen`, `onboarding:pager`, `onboarding:indicator`, `onboarding:nextButton`, `onboarding:skipButton`
- Do NOT modify `OnboardingState.kt`, `OnboardingViewModel.kt`, `OnboardingRoute.kt`, or any navigation code
- Do NOT modify `MoneyManagerTypography` or `MoneyManagerExtendedTypography`
- Use `.copy(alpha = 0.07f)` for GreenLight opacity (more readable than raw hex alpha)
