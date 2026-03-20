---
description: "Pencil MCP tools for reading, creating, and referencing UI designs in .pen files. Use when implementing UI features with design mockups or creating new screen designs."
---

# Pencil MCP — Design Reference & Creation

## Context

The project uses Pencil (.pen files) for UI design mockups. The contents of .pen files are encrypted — you MUST use Pencil MCP tools exclusively. Never use Read, Grep, or Bash to access .pen file contents.

## Reading Designs (for implementation / speckit agents)

Use this workflow when you need to understand an existing design before implementing it in code.

### Step 1: Discover what's open

```
mcp__pencil__get_editor_state(include_schema: false)
  → returns active .pen file, top-level frames (screens), and reusable components
```

### Step 2: Visual inspection

```
mcp__pencil__get_screenshot(nodeId: "<frame-id>")
  → returns PNG screenshot of a specific frame/screen/component
```

### Step 3: Read structure, colors, spacing, fonts

```
mcp__pencil__batch_get(nodeIds: ["<frame-id>"], readDepth: 3)
  → returns full node tree with properties: fill, fontSize, fontWeight, gap, padding, cornerRadius, etc.
  → increase readDepth for deeper nesting, but avoid >5 to prevent context overflow
```

### Step 4: Check computed layout dimensions

```
mcp__pencil__snapshot_layout(parentId: "<frame-id>", maxDepth: 2)
  → returns computed x, y, width, height rectangles for each node
```

### Example: extracting design tokens for Compose implementation

```
1. get_editor_state → find the screen frame ID
2. get_screenshot → see overall design visually
3. batch_get(readDepth: 4) → extract exact values:
   - fill: "#242322" → Dark surface color
   - fontSize: 18, fontWeight: "600" → SemiBold section header
   - cornerRadius: 16 → Card corner radius
   - gap: 24 → Section spacing
   - padding: [20, 20] → Card internal padding
4. Map these to your Compose theme/tokens
```

## Creating & Editing Designs

Use this when designing new screens or modifying existing mockups.

### Get creative direction first

```
mcp__pencil__get_guidelines(topic: "mobile-app")   # or "web-app", "design-system", "landing-page"
mcp__pencil__get_style_guide_tags()                 # list available style tags
mcp__pencil__get_style_guide(tags: ["mobile", "fintech", "clean", "dark-mode", "modern"])
```

### Design operations

```
mcp__pencil__batch_design(operations: "...")
  → insert (I), copy (C), update (U), replace (R), move (M), delete (D), generate image (G)
  → maximum 25 operations per call
  → MUST set placeholder: true on a frame BEFORE working inside it
  → MUST remove placeholder: true AFTER finishing the frame
```

### Export to images

```
mcp__pencil__export_nodes(nodeIds: ["<id1>", "<id2>"], outputDir: "/path/to/dir", format: "png")
  → exports frames as images (2x scale by default)
  → useful for creating PNG fallbacks alongside .pen designs
```

## Passing Design References to Other Agents

When another agent (e.g., speckit, implementation) needs to reference a design, include this in the prompt:

```
Design reference: Use Pencil MCP tools to inspect the design.
1. Call get_editor_state() to find screen frames by name (e.g., "Statistics Screen")
2. Call get_screenshot(nodeId: "<frame-id>") for visual reference
3. Call batch_get(nodeIds: ["<frame-id>"], readDepth: 4) for exact colors, spacing, fonts
PNG fallbacks (if editor is unavailable): specs/<feature>/design/<filename>.png
```

Key principle: reference frames **by name**, not by node ID — IDs may change if the design is recreated. The agent should use `get_editor_state` to discover current IDs by matching frame names.

## Anti-patterns

- NEVER read .pen files via Read/Grep/Bash — content is encrypted, use only Pencil MCP tools
- NEVER set width/height on text without setting textGrowth first — dimensions will be ignored
- NEVER forget placeholder: true when working inside a frame — and always remove it after
- NEVER exceed 25 operations in a single batch_design call — split into multiple calls
- NEVER hardcode node IDs in persistent docs (specs, skills) — IDs are ephemeral, use frame names
- NEVER use Update (U) on descendants of a just-copied (C) node — copy creates new child IDs
