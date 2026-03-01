# PRD: Fix CategoryBottomSheet & Add Roborazzi Screenshot Tests

## Introduction

CategoryBottomSheet на экране создания транзакций имеет полупрозрачный фон (`containerColor = Color.Transparent`), что приводит к визуальным артефактам. Нужно сделать фон солидным и покрыть компонент Roborazzi screenshot-тестами. Также нужно починить существующий FAB screenshot-тест, который помечен `@Ignore`.

## Goals

- Исправить полупрозрачный фон CategoryBottomSheet — сделать solid surface color
- Убрать `@Ignore` с существующего FAB screenshot-теста и убедиться, что он проходит
- Добавить Roborazzi screenshot-тесты для CategoryBottomSheet в разных состояниях
- Добавить Roborazzi screenshot-тест для TransactionEditScreen (базовое состояние)

## User Stories

### US-001: Fix CategoryBottomSheet transparent background
**Description:** As a user, I want the category bottom sheet to have a solid background so that content behind it is not visible through the sheet.

**Acceptance Criteria:**
- [ ] `ModalBottomSheet` в `CategoryBottomSheet` использует solid `containerColor` вместо `Color.Transparent`
- [ ] Убрать ручной `Column` с `background()` внутри sheet — цвет должен задаваться через `containerColor` самого `ModalBottomSheet`
- [ ] Фон корректно отображается в dark theme (`SurfaceDark`) и light theme (`SurfaceLight`)
- [ ] `./gradlew assembleDebug` passes

### US-002: Fix existing FAB Roborazzi test
**Description:** As a developer, I want the existing FAB screenshot test to run without `@Ignore` so that CI catches visual regressions.

**Acceptance Criteria:**
- [ ] Удалить `@Ignore` аннотацию из `MoneyManagerFABScreenshotTest`
- [ ] Удалить неиспользуемый import `org.junit.Ignore`
- [ ] Тест `captureFloatingActionButton` проходит при запуске `./gradlew :app:recordRoborazziDebug`
- [ ] `./gradlew :app:testDebugUnitTest` passes

### US-003: Add Roborazzi screenshot tests for CategoryBottomSheet
**Description:** As a developer, I want screenshot tests for CategoryBottomSheet to catch visual regressions in the category selection UI.

**Acceptance Criteria:**
- [ ] Тест для CategoryBottomSheet с пустым списком категорий
- [ ] Тест для CategoryBottomSheet с категориями (3+ штуки) без выбранной
- [ ] Тест для CategoryBottomSheet с выбранной категорией (isSelected = true)
- [ ] Все тесты проходят `./gradlew :app:recordRoborazziDebug`
- [ ] Скриншоты сохраняются в `app/src/test/screenshots/`
- [ ] `./gradlew :app:testDebugUnitTest` passes

### US-004: Add Roborazzi screenshot test for TransactionEditScreen
**Description:** As a developer, I want a baseline screenshot test for the transaction edit screen to catch layout regressions.

**Acceptance Criteria:**
- [ ] Тест для TransactionEditScreen в состоянии "новая транзакция" (пустая форма, isLoading = false)
- [ ] Тест для TransactionEditScreen с заполненными полями (amount, category selected, note)
- [ ] Тесты проходят `./gradlew :app:recordRoborazziDebug`
- [ ] Скриншоты сохраняются в `app/src/test/screenshots/`
- [ ] `./gradlew :app:testDebugUnitTest` passes

## Functional Requirements

- FR-1: `CategoryBottomSheet` должен использовать `MaterialTheme.colorScheme.surface` или `SurfaceDark/SurfaceLight` как `containerColor` в `ModalBottomSheet`
- FR-2: Все Roborazzi-тесты используют `@RunWith(RobolectricTestRunner::class)`, `@GraphicsMode(GraphicsMode.Mode.NATIVE)`, `@Config(sdk = [33], qualifiers = "xxhdpi")`
- FR-3: Тесты оборачивают composable в `MoneyManagerTheme(dynamicColor = false)`
- FR-4: `CategoryBottomSheet` — private composable, поэтому тесты должны рендерить `TransactionEditScreen` с `state.showCategorySheet = true`
- FR-5: Скриншоты именуются описательно: `category_sheet_empty.png`, `category_sheet_with_items.png`, `category_sheet_selected.png`, `transaction_edit_empty.png`, `transaction_edit_filled.png`

## Non-Goals

- Тесты для DatePickerDialog (отдельная задача)
- Тесты для других экранов (accounts, statistics и т.д.)
- CI-интеграция Roborazzi (уже работает через `./gradlew testDebugUnitTest`)
- Параметризованные тесты для разных размеров экрана

## Technical Considerations

- `CategoryBottomSheet` — `private` composable внутри `TransactionEditScreen.kt`. Для тестирования нужно рендерить `TransactionEditScreen` с нужным `TransactionEditState`
- `TransactionEditScreen` — stateless composable, принимает `TransactionEditState` и лямбды — идеально для screenshot-тестов
- Roborazzi инфраструктура уже настроена в `app/build.gradle.kts` (plugin + dependencies)
- Существующий `MoneyManagerFABScreenshotTest` — пример working setup
- Тесты живут в `app/src/test/kotlin/` (unit test source set с Robolectric)
- Для создания тестовых категорий нужна модель `Category` из `core:model`

## Success Metrics

- CategoryBottomSheet имеет непрозрачный (solid) фон
- Все screenshot-тесты проходят без `@Ignore`
- 5+ screenshot файлов генерируются в `app/src/test/screenshots/`
- `./gradlew :app:testDebugUnitTest` — зелёный
