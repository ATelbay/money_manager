---
description: "Generating UI tests for Money Manager: Compose Testing, ComposeTestRule, testTag, screen coverage, happy path, error states, empty states, edge cases"
---

# Generate UI Test

## Context

Money Manager is the foundation for a master's thesis on UI automation testing. Every screen must be covered with UI tests. Tests reside in the `androidTest` source set of the corresponding feature module.

**Key files:**
- `presentation/*/src/androidTest/` — UI tests (instrumented, with Compose)
- `presentation/*/src/test/` — Unit tests (ViewModel, UseCase — no Android)
- `presentation/*/src/.../ui/*Screen.kt` — screens (source of testTags)
- `presentation/*/src/.../ui/*State.kt` — states (source of test data)

## Process

### Step 1: Analyze the screen
1. Read `{Feature}Screen.kt` — find all `testTag("...")` via Grep
2. Read `{Feature}State.kt` — understand the screen's data structure
3. Identify the list of interactive elements and their behavior

### Step 2: Prepare the test class
```kotlin
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class {Feature}ScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }
}
```

### Step 3: Write tests by category

**A. Happy Path (main scenario)**
- Screen displays correctly with valid data
- All elements are visible and clickable
- Navigation works

**B. Error States**
- Form with invalid data shows errors
- Network error displays error message
- Retry button works

**C. Empty States**
- Empty list shows placeholder
- Absence of data does not crash the screen

**D. Edge Cases**
- Very long text in fields
- Minimum/maximum numeric values
- Fast double-tap on buttons
- Screen rotation (if applicable)

### Step 4: Assertions and actions
```kotlin
// Search by testTag
composeTestRule.onNodeWithTag("transactionList:fab").assertIsDisplayed()
composeTestRule.onNodeWithTag("transactionEdit:amountField").performTextInput("100.50")

// Search by text
composeTestRule.onNodeWithText("Сохранить").performClick()

// Search by content description
composeTestRule.onNodeWithContentDescription("Удалить").performClick()

// Lists
composeTestRule.onNodeWithTag("transactionList:list")
    .onChildren()
    .assertCountEquals(5)

// Wait for appearance
composeTestRule.waitUntilExactlyOneExists(
    hasTestTag("transactionList:item_0"),
    timeoutMillis = 5000
)
```

## testTag Convention

Format: `"screen:element"` or `"screen:element_index"` for lists.

| Screen | testTag examples |
|--------|-----------------|
| TransactionList | `transactionList:fab`, `transactionList:item_{id}` |
| TransactionEdit | `transactionEdit:amountField`, `transactionEdit:saveButton`, `transactionEdit:categoryPicker` |
| Import | `import:selectPdf`, `import:takePhoto`, `import:importButton`, `import:preview` |
| Settings | `settings:themeSelector` |
| CategoryList | `categoryList:fab`, `categoryList:item_{id}` |

## Quality Bar

- Minimum 3 tests per screen: happy path, error state, empty state
- Every `testTag` in a Screen must be tested in at least one test
- Tests do NOT depend on each other (each is isolated)
- Use `createAndroidComposeRule`, NOT `createComposeRule`, for Hilt integration
- Create test data via factories/builders, do not hardcode

## Anti-patterns

- Do NOT test implementation details — test behavior visible to the user
- Do NOT use `Thread.sleep()` — use `waitUntil*` or `IdlingResource`
- Do NOT rely on test execution order
- Do NOT test ViewModel logic in UI tests — use unit tests for that
- Do NOT mock Compose components — mock data/repositories via Hilt testing
- Do NOT forget `assertIsDisplayed()` before `performClick()` — the element may be off-screen
