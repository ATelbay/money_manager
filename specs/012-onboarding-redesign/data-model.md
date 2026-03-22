# Data Model: Onboarding Screens Redesign

**Date**: 2026-03-21
**Branch**: `012-onboarding-redesign`

## No Data Model Changes

This feature is a pure UI redesign. No entities, database tables, DAOs, or data layer code are modified.

### Existing Entities (unchanged)

- **OnboardingState**: `data class` with `currentPage: Int = 0`. No changes.
- **OnboardingPage**: `data class` with `title: String`, `description: String`, `icon: String`. No changes.
- **DataStore key**: `onboarding_completed` (Boolean). No changes.

### New Type Definitions

- **OutfitFontFamily**: `FontFamily` constant added to `Type.kt` in `core:ui`. Not a data entity — a UI resource definition.
- **Onboarding color constants**: 7 `Color` vals local to `OnboardingScreen.kt`. Not persisted or shared.
