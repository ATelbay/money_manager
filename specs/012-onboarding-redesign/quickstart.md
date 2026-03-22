# Quickstart: Onboarding Screens Redesign

**Branch**: `012-onboarding-redesign`

## Prerequisites

1. Android Studio with Kotlin 2.3.0 and Compose BOM 2026.01.01
2. Download Outfit font files from Google Fonts (OFL license):
   - `Outfit-Regular.ttf` → rename to `outfit_regular.ttf`
   - `Outfit-Medium.ttf` → rename to `outfit_medium.ttf`
   - `Outfit-SemiBold.ttf` → rename to `outfit_semibold.ttf`

## Setup

```bash
git checkout 012-onboarding-redesign

# Create font directory (doesn't exist yet)
mkdir -p core/ui/src/main/res/font/

# Copy font files (after downloading from Google Fonts)
cp outfit_regular.ttf core/ui/src/main/res/font/
cp outfit_medium.ttf core/ui/src/main/res/font/
cp outfit_semibold.ttf core/ui/src/main/res/font/
```

## Build & Verify

```bash
./gradlew assembleDebug
```

## Files to Modify

1. **`core/ui/src/main/res/font/`** — Add 3 .ttf files
2. **`core/ui/.../theme/Type.kt`** — Add `OutfitFontFamily` definition (do NOT touch `MoneyManagerTypography`)
3. **`presentation/onboarding/.../ui/OnboardingScreen.kt`** — Redesign composables with new colors, typography, layout

## Visual Verification

Use Pencil MCP to compare implementation against design:
- Page 1: `get_screenshot("vq5yF")`
- Page 2: `get_screenshot("FuS5O")`
- Page 3: `get_screenshot("LXNau")`

Run on emulator and take screenshots to compare side-by-side.

## Test Tags to Preserve

- `onboarding:screen`
- `onboarding:pager`
- `onboarding:indicator`
- `onboarding:nextButton`
- `onboarding:skipButton`
