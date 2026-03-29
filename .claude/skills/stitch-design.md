---
description: "Stitch MCP — AI generation of UI screens from text prompts, design system management (colors, fonts, shapes), creating design variants. Use when prototyping new screens or exploring alternative layouts."
---

# Stitch MCP — AI Screen Generation and Design Systems

## Context

Stitch is an AI tool for generating UI screens from text descriptions. Unlike Pencil (manual .pen mockups with exact values), Stitch creates designs programmatically via Gemini.

**Stitch vs Pencil:**

| | Stitch | Pencil |
|--|--------|--------|
| Purpose | Generate and explore designs | Read/edit .pen mockups |
| Input | Text prompt | Manual design |
| Output | Ready screens + variants | Exact values (px, hex, dp) |
| When | Prototyping, layout exploration | Code implementation from a finished mockup |

## Core Entities

| Entity | Description | Resource Format |
|--------|-------------|-----------------|
| **Project** | Container for screens and design systems | `projects/{id}` |
| **Screen** | Individual UI screen in a project | `projects/{project}/screens/{screen}` |
| **Screen Instance** | Placed screen instance on canvas (own `id` + `sourceScreen`) | — |
| **Design System** | Design tokens (colors, fonts, roundness) | `assets/{id}` |

**Important:** Project ID is digits only (e.g. `4044680601076201931`), WITHOUT the `projects/` prefix. Same for screen ID and asset ID.

## Project Management

```
# List projects
mcp__stitch__list_projects()
mcp__stitch__list_projects(filter: "view=shared")  # others' shared projects

# Create a project
mcp__stitch__create_project(title: "Money Manager Redesign")
  → returns name: "projects/{id}"

# Project details (including screen instances for apply_design_system)
mcp__stitch__get_project(name: "projects/{id}")
  → returns screenInstances: [{id, sourceScreen}]
```

## Design System

### Step 1: Create

```
mcp__stitch__create_design_system(
  projectId: "{id}",
  designSystem: {
    displayName: "Money Manager DS",
    theme: {
      colorMode: "DARK",
      headlineFont: "INTER",
      bodyFont: "INTER",
      roundness: "ROUND_TWELVE",
      customColor: "#4CAF50"
    }
  }
)
  → returns name: "assets/{asset_id}"
```

### Step 2: Update (REQUIRED after creation!)

Without calling `update_design_system`, the design system will not be applied.

```
mcp__stitch__update_design_system(
  name: "assets/{asset_id}",
  projectId: "{id}",
  designSystem: { ...same parameters... }
)
```

### Step 3: Apply to Screens

```
# First get screen instances from the project
mcp__stitch__get_project(name: "projects/{id}")
  → screenInstances: [{id: "inst_123", sourceScreen: "projects/{id}/screens/{scr_id}"}]

# Then apply
mcp__stitch__apply_design_system(
  projectId: "{id}",
  assetId: "{asset_id}",
  selectedScreenInstances: [
    { id: "inst_123", sourceScreen: "projects/{id}/screens/{scr_id}" }
  ]
)
```

### Reference: List Design Systems

```
mcp__stitch__list_design_systems(projectId: "{id}")
```

### Theme Parameters

**Required:**

| Parameter | Values |
|-----------|--------|
| `colorMode` | `LIGHT`, `DARK` |
| `headlineFont` | see font table below |
| `bodyFont` | see font table below |
| `roundness` | `ROUND_FOUR`, `ROUND_EIGHT`, `ROUND_TWELVE`, `ROUND_FULL` |
| `customColor` | hex color, seed for palette (e.g. `"#4CAF50"`) |

**Optional:**

| Parameter | Description |
|-----------|-------------|
| `labelFont` | Font for labels (defaults to bodyFont) |
| `colorVariant` | `MONOCHROME`, `NEUTRAL`, `TONAL_SPOT`, `VIBRANT`, `EXPRESSIVE`, `FIDELITY`, `CONTENT`, `RAINBOW`, `FRUIT_SALAD` |
| `overridePrimaryColor` | Hex — overrides primary from dynamic palette |
| `overrideSecondaryColor` | Hex — overrides secondary |
| `overrideTertiaryColor` | Hex — overrides tertiary |
| `overrideNeutralColor` | Hex — overrides neutral |
| `designMd` | Markdown description of the design system (free form) |

### Available Fonts

Sans-serif: `INTER`, `DM_SANS`, `PLUS_JAKARTA_SANS`, `MANROPE`, `LEXEND`, `SPACE_GROTESK`, `WORK_SANS`, `MONTSERRAT`, `RUBIK`, `GEIST`, `SORA`, `IBM_PLEX_SANS`, `NUNITO_SANS`, `SOURCE_SANS_THREE`, `HANKEN_GROTESK`, `BE_VIETNAM_PRO`, `EPILOGUE`, `PUBLIC_SANS`, `SPLINE_SANS`, `METROPOLIS`, `ARIMO`

Serif: `NEWSREADER`, `NOTO_SERIF`, `DOMINE`, `LIBRE_CASLON_TEXT`, `EB_GARAMOND`, `LITERATA`, `SOURCE_SERIF_FOUR`

## Screen Generation

```
mcp__stitch__generate_screen_from_text(
  projectId: "{id}",
  prompt: "Transaction list screen with grouping by date...",
  deviceType: "MOBILE",
  modelId: "GEMINI_3_1_PRO"
)
```

**deviceType:** `MOBILE`, `DESKTOP`, `TABLET`, `AGNOSTIC`

**modelId:** `GEMINI_3_1_PRO` (better quality) | `GEMINI_3_FLASH` (faster)

**Generation takes several minutes.** Do NOT retry on timeout — the process may complete successfully. Check the result via `get_screen` later.

**If `output_components` contains suggestions** (e.g. "Yes, make them all") — show them to the user. If they select an option, call `generate_screen_from_text` again with the selected suggestion in `prompt`.

### Prompt Tips (Money Manager)

- Mention Material 3 components explicitly: "Material 3 top app bar", "FAB", "navigation bar"
- Use realistic content: amounts in KZT (`- 5 400 ₸`), category names, icons
- Specify theme (dark/light) if a design system is set
- Describe navigation: "bottom bar: Transactions, Statistics, Add, Import, Settings"
- For fintech apps: "clean card-based layout, minimal, professional"

## Editing Screens

```
mcp__stitch__edit_screens(
  projectId: "{id}",
  selectedScreenIds: ["{screen_id_1}", "{screen_id_2}"],
  prompt: "Increase spacing between list items to 12dp, darken card backgrounds",
  deviceType: "MOBILE",
  modelId: "GEMINI_3_1_PRO"
)
```

Takes several minutes. Do NOT retry on timeout.

## Generating Variants

Explore alternative designs for existing screens.

```
mcp__stitch__generate_variants(
  projectId: "{id}",
  selectedScreenIds: ["{screen_id}"],
  prompt: "Explore different card layouts for transactions",
  deviceType: "MOBILE",
  modelId: "GEMINI_3_1_PRO",
  variantOptions: {
    variantCount: 3,
    creativeRange: "EXPLORE",
    aspects: ["LAYOUT", "COLOR_SCHEME"]
  }
)
```

| Parameter | Values |
|-----------|--------|
| `variantCount` | 1–5 (default 3) |
| `creativeRange` | `REFINE` (minimal changes), `EXPLORE` (balanced), `REIMAGINE` (radical) |
| `aspects` | `LAYOUT`, `COLOR_SCHEME`, `IMAGES`, `TEXT_FONT`, `TEXT_CONTENT` (multiple allowed) |

## Reading Project State

```
# List screens
mcp__stitch__list_screens(projectId: "{id}")

# Screen details
mcp__stitch__get_screen(
  name: "projects/{project_id}/screens/{screen_id}",
  projectId: "{project_id}",
  screenId: "{screen_id}"
)
```

**Note:** `get_screen` requires all three parameters: `name` (full path), `projectId`, `screenId`.

## Integration with Speckit and Pencil

### Workflow

1. **Stitch** — rapid prototyping: generate screens from a feature description
2. **Pencil** — precise refinement: manual layout editing, extracting exact values for code
3. **Speckit** — planning: reference Stitch project/screen in spec.md and tasks.md

### In spec.md

```markdown
## Design Reference
- Stitch project: `{project_id}` (title: "Feature Name")
- Screens: "Transaction List", "Add Transaction" (reference by name, not by ID)
- For exact values use Pencil MCP batch_get after export
```

### In tasks.md

Include the Stitch screen name in each UI task:
```markdown
- [ ] Implement TransactionListScreen (ref: Stitch screen "Transaction List")
```

## Anti-patterns

- Do NOT retry `generate_screen_from_text` / `edit_screens` on timeout — generation is async, check via `get_screen`
- Do NOT skip `update_design_system` after `create_design_system` — the DS won't be applied without it
- Do NOT use `GEMINI_3_PRO` — deprecated, use only `GEMINI_3_1_PRO` or `GEMINI_3_FLASH`
- Do NOT confuse screen instance `id` with `sourceScreen` — `apply_design_system` requires instance ID from `get_project`
- Do NOT pass the `projects/` / `assets/` prefix where only the ID is expected (and vice versa)
- Do NOT hardcode Stitch screen IDs in permanent documents — screens get regenerated, reference by name instead
