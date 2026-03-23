# Research: Onboarding Screens Redesign

**Date**: 2026-03-21
**Branch**: `012-onboarding-redesign`

## R1: Outfit Font Integration in Android/Compose

**Decision**: Add Outfit .ttf files (Regular, Medium, SemiBold) to `core/ui/src/main/res/font/` and define `OutfitFontFamily` in `Type.kt` using `FontFamily(Font(R.font.outfit_regular, FontWeight.Normal), ...)`.

**Rationale**: Standard Android resource font approach. Compose's `FontFamily` with explicit `Font` entries per weight is the canonical pattern. No need for downloadable fonts or XML font families since only 3 weights are needed and they're bundled.

**Alternatives considered**:
- Downloadable fonts via Google Fonts provider — rejected: adds network dependency for a first-launch screen, increases complexity.
- XML font family resource (`@font/outfit`) — rejected: unnecessary extra file; Compose `FontFamily` constructor is simpler and sufficient.
- Adding Outfit to global `MoneyManagerTypography` — rejected: spec explicitly requires no global typography changes.

## R2: Onboarding-Local Color Constants

**Decision**: Define colors as top-level `private val` constants in `OnboardingScreen.kt` using `Color(0xFF3D8A5A)` etc.

**Rationale**: These colors are specific to the onboarding brand identity and not part of the Material theme system. Local constants avoid polluting the global theme while keeping the values discoverable in one file.

**Alternatives considered**:
- Adding to `MaterialTheme.colorScheme` custom extension — rejected: would require custom `CompositionLocal`, overkill for one screen.
- Separate `OnboardingColors.kt` file — rejected: only 6 color values, not worth a separate file.

## R3: Replacing MoneyManagerButton with Custom Button

**Decision**: Replace `MoneyManagerButton` usage with an inline `Button` composable styled with onboarding-specific colors and shape. Replace `MoneyManagerTextButton` with a plain `Text` + `clickable` modifier.

**Rationale**: The existing `MoneyManagerButton` uses Teal color, 56dp height, and press-scale animation. The design requires green (#3D8A5A), 52dp height, 14dp rounded corners, and no press animation. Creating a local composable or inline styling is cleaner than parameterizing the shared button.

**Alternatives considered**:
- Parameterizing `MoneyManagerButton` with color/shape overrides — rejected: increases shared component complexity for a single consumer.
- Creating `OnboardingButton` as a reusable component in `core:ui` — rejected: only used in one screen.

## R4: GreenLight Opacity Interpretation

**Decision**: `#3D8A5A12` means Color(0x123D8A5A) — alpha 0x12 (18/255 ≈ 7%) with RGB #3D8A5A. In Compose: `Color(0x123D8A5A)`.

**Rationale**: The "12" suffix in the hex value represents the alpha channel in ARGB format. This matches the design screenshot showing a very light green tint behind the icon.

**Alternatives considered**:
- `Color(0xFF3D8A5A).copy(alpha = 0.07f)` — equivalent and more readable, either approach is fine. Using `.copy(alpha = 0.07f)` is preferred for clarity.
