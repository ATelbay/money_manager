---
description: "Генерация UI-тестов для Money Manager: Compose Testing, ComposeTestRule, testTag, покрытие экранов, happy path, error states, empty states, edge cases"
---

# Generate UI Test

## Context

Money Manager — база для магистерской диссертации по UI-автоматизации тестирования. Каждый экран должен быть покрыт UI-тестами. Тесты располагаются в `androidTest` source set соответствующего feature-модуля.

**Ключевые файлы:**
- `feature/*/src/androidTest/` — UI-тесты
- `feature/*/src/.../ui/*Screen.kt` — экраны (источник testTag'ов)
- `feature/*/src/.../ui/*State.kt` — стейты (источник тестовых данных)

## Process

### Шаг 1: Анализ экрана
1. Прочитай `{Feature}Screen.kt` — найди все `testTag("...")` через Grep
2. Прочитай `{Feature}State.kt` — пойми структуру данных экрана
3. Определи список интерактивных элементов и их поведение

### Шаг 2: Подготовка тестового класса
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

### Шаг 3: Написать тесты по категориям

**A. Happy Path (основной сценарий)**
- Экран отображается корректно с валидными данными
- Все элементы видимы и кликабельны
- Навигация работает

**B. Error States**
- Форма с невалидными данными показывает ошибки
- Сетевая ошибка отображает error message
- Retry кнопка работает

**C. Empty States**
- Пустой список показывает placeholder
- Отсутствие данных не крашит экран

**D. Edge Cases**
- Очень длинный текст в полях
- Минимальные/максимальные числовые значения
- Быстрый double-tap на кнопки
- Поворот экрана (если applicable)

### Шаг 4: Assertions и actions
```kotlin
// Поиск по testTag
composeTestRule.onNodeWithTag("transactionList:fab").assertIsDisplayed()
composeTestRule.onNodeWithTag("transactionEdit:amountField").performTextInput("100.50")

// Поиск по тексту
composeTestRule.onNodeWithText("Сохранить").performClick()

// Поиск по content description
composeTestRule.onNodeWithContentDescription("Удалить").performClick()

// Списки
composeTestRule.onNodeWithTag("transactionList:list")
    .onChildren()
    .assertCountEquals(5)

// Ожидание появления
composeTestRule.waitUntilExactlyOneExists(
    hasTestTag("transactionList:item_0"),
    timeoutMillis = 5000
)
```

## testTag Convention

Формат: `"screen:element"` или `"screen:element_index"` для списков.

| Экран | Примеры testTag |
|-------|-----------------|
| TransactionList | `transactionList:fab`, `transactionList:item_{id}` |
| TransactionEdit | `transactionEdit:amountField`, `transactionEdit:saveButton`, `transactionEdit:categoryPicker` |
| Import | `import:selectPdf`, `import:takePhoto`, `import:importButton`, `import:preview` |
| Settings | `settings:themeSelector` |
| CategoryList | `categoryList:fab`, `categoryList:item_{id}` |

## Quality Bar

- Минимум 3 теста на экран: happy path, error state, empty state
- Каждый `testTag` в Screen должен быть протестирован хотя бы в одном тесте
- Тесты НЕ зависят друг от друга (каждый — изолированный)
- Используй `createAndroidComposeRule`, НЕ `createComposeRule`, для Hilt-интеграции
- Тестовые данные создавай через фабрики/builders, не хардкодь

## Anti-patterns

- НЕ тестируй implementation details — тестируй поведение, видимое пользователю
- НЕ используй `Thread.sleep()` — используй `waitUntil*` или `IdlingResource`
- НЕ полагайся на порядок выполнения тестов
- НЕ тестируй ViewModel логику в UI-тестах — для этого есть unit-тесты
- НЕ мокай Compose-компоненты — мокай данные/репозитории через Hilt testing
- НЕ забывай про `assertIsDisplayed()` перед `performClick()` — элемент может быть off-screen
