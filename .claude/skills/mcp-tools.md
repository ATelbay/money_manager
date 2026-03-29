---
description: "MCP tools in Money Manager: context7 for library documentation lookup, Firebase MCP for project operations, Playwright — not applicable for Android"
---

# MCP Tools

## Context

Several MCP servers are available in the project. Use the right tool for the right task — it's faster and more precise than general-purpose approaches.

## context7 — Library Documentation (preferred method)

**When to use:** looking up API documentation and code examples for any library in the project. Faster and more accurate than WebSearch for library docs.

**Two steps:**

### Step 1: Find libraryId
```
mcp__context7__resolve-library-id
  query: "jetpack compose navigation"
  → returns libraryId, e.g. "/androidx/navigation"
```

### Step 2: Query documentation
```
mcp__context7__query-docs
  libraryId: "/androidx/navigation"
  query: "type-safe destinations composable"
  → returns up-to-date documentation with code examples
```

**Example queries for this project:**

| Task | resolve query | docs query |
|------|--------------|------------|
| Type-safe Navigation | `"navigation-compose"` | `"type-safe composable destinations"` |
| Room new annotations | `"androidx room"` | `"@Upsert @MapColumn"` |
| Hilt + Compose | `"hilt android"` | `"hiltViewModel ViewModelComponent"` |
| Vico 2.x charts | `"vico charts compose"` | `"CartesianChartHost rememberCartesianChartModel"` |
| Turbine Flow testing | `"turbine"` | `"test awaitItem awaitComplete"` |
| Coil 3 AsyncImage | `"coil"` | `"AsyncImage ImageRequest compose"` |
| Material 3 components | `"compose material3"` | `"ExposedDropdownMenuBox SegmentedButton"` |
| Firebase AI Gemini | `"firebase-ai-logic"` | `"generateContent GenerativeModel"` |
| CredentialManager | `"androidx credentials"` | `"GetCredentialRequest GetGoogleIdOption"` |

**context7 vs WebSearch:**
- context7: API docs, method signatures, official examples
- WebSearch: Stack Overflow, GitHub Issues, blog posts, changelogs

## Firebase MCP — Firebase Project Operations

**When to use:** configuring the Firebase project, adding SHA fingerprints, checking configuration, reading security rules.

### Available tools

| Tool | Purpose |
|------|---------|
| `mcp__plugin_firebase_firebase__firebase_list_projects` | List Firebase projects |
| `mcp__plugin_firebase_firebase__firebase_get_project` | Project details |
| `mcp__plugin_firebase_firebase__firebase_list_apps` | List apps in a project |
| `mcp__plugin_firebase_firebase__firebase_get_sdk_config` | Get google-services.json config |
| `mcp__plugin_firebase_firebase__firebase_get_security_rules` | Read Firestore/Storage rules |
| `mcp__plugin_firebase_firebase__firebase_create_android_sha` | Add SHA fingerprint |
| `mcp__plugin_firebase_firebase__firebase_get_environment` | Firebase environment info |
| `mcp__plugin_firebase_firebase__firebase_read_resources` | Read project resources |

### Example: adding a debug SHA fingerprint

```bash
# Step 1: Get SHA from Gradle
./gradlew signingReport
# Find SHA-1 and SHA-256 for the debug variant

# Step 2: Add via Firebase MCP
mcp__plugin_firebase_firebase__firebase_create_android_sha
  appId: "your-app-id"
  shaHash: "SHA-1 or SHA-256 from signingReport"
  hashType: "SHA_1"  # or "SHA_256"
```

### Example: verifying SDK configuration

```
mcp__plugin_firebase_firebase__firebase_get_sdk_config
  appId: "android-app-id"
  → returns the contents of google-services.json
```

## Playwright MCP — NOT applicable for Android

**Playwright tools are designed for web browsers. DO NOT use for Android.**

| Task needed | Correct tool |
|-------------|-------------|
| Run UI tests | `./gradlew connectedAndroidTest` |
| Inspect layout | Android Studio Layout Inspector or `adb shell screencap` |
| Interact with emulator | `adb shell input tap X Y` or `./gradlew installDebug` |
| Screenshot | `adb exec-out screencap -p > screen.png` |

## IDE MCP — Diagnostics

```
mcp__ide__getDiagnostics
  → returns compiler errors/warnings without a full Gradle build
```

Useful for a quick type-check before running `./gradlew assembleDebug`.

## Quick Reference

| Task | Tool |
|------|------|
| Library API documentation | context7: resolve → query |
| Code example for Compose API | context7 → resolve "jetpack compose" |
| Add SHA fingerprint | `firebase_create_android_sha` |
| Check Firebase config | `firebase_get_sdk_config` |
| Read security rules | `firebase_get_security_rules` |
| Compiler errors | `mcp__ide__getDiagnostics` |
| Android UI testing | `./gradlew connectedAndroidTest` |
| Stack Overflow / GitHub Issues | WebSearch |

## Anti-patterns

- DO NOT use Playwright for Android — these tools are for web browsers
- DO NOT skip `resolve-library-id` before `query-docs` — without libraryId the query will not work
- DO NOT search for Android documentation via WebSearch if context7 can handle it faster
- DO NOT use Firebase MCP to modify production security rules without a review
