---
description: "Unit-тестирование в Money Manager: ViewModel тесты, UseCase тесты, MockK для моков, Turbine для Flow, MainDispatcherRule, размещение тестов по модулям"
---

# Unit Testing

## Context

Money Manager — база для магистерской диссертации по UI-автоматизации, поэтому тестовое покрытие критично. Скилл описывает **unit-тесты** (JVM, без Android), в отличие от **UI-тестов** (instrumented, `androidTest/`).

**Зависимости (из `libs.versions.toml`):**
```toml
turbine = "1.2.1"        # Flow testing
mockk = "1.14.9"         # Kotlin mocking
coroutines = "1.10.2"    # включает kotlinx-coroutines-test
junit = "4.13.2"
```

## Размещение тестов

| Тип теста | Директория | Запуск |
|-----------|-----------|--------|
| Unit (ViewModel, UseCase, Repository) | `{module}/src/test/` | `./gradlew test` (JVM) |
| UI/Instrumented | `presentation/*/src/androidTest/` | `./gradlew connectedAndroidTest` |

Примеры:
- `domain/transactions/src/test/` — тесты UseCase
- `data/transactions/src/test/` — тесты mapper, RepositoryImpl (с моком DAO)
- `presentation/transactions/src/test/` — тесты ViewModel

**build.gradle.kts** (добавить в нужный модуль):
```kotlin
dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
```

## MainDispatcherRule

Обязательна для тестов ViewModel — заменяет `Dispatchers.Main` на тестовый диспетчер.

```kotlin
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(testDispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}
```

## Тесты ViewModel

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class TransactionListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getTransactionsUseCase: GetTransactionsUseCase = mockk()
    private lateinit var viewModel: TransactionListViewModel

    @Before
    fun setup() {
        every { getTransactionsUseCase() } returns flowOf(emptyList())
        viewModel = TransactionListViewModel(getTransactionsUseCase)
    }

    @Test
    fun `initial state has empty list and isLoading false`() = runTest {
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.transactions.isEmpty())
            assertFalse(state.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `transactions are loaded and mapped to state`() = runTest {
        val transactions = listOf(
            Transaction(id = 1, amount = 100.0, type = TransactionType.EXPENSE)
        )
        every { getTransactionsUseCase() } returns flowOf(transactions)
        viewModel = TransactionListViewModel(getTransactionsUseCase)

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(1, state.transactions.size)
            assertEquals(100.0, state.transactions.first().amount, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `error from use case sets error state`() = runTest {
        every { getTransactionsUseCase() } returns flow { throw RuntimeException("DB error") }
        viewModel = TransactionListViewModel(getTransactionsUseCase)

        viewModel.state.test {
            val state = awaitItem()
            assertNotNull(state.error)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

## Тесты UseCase

```kotlin
class GetTransactionsUseCaseTest {

    private val repository: TransactionRepository = mockk()
    private val useCase = GetTransactionsUseCase(repository)

    @Test
    fun `invoke delegates to repository`() = runTest {
        val transactions = listOf(Transaction(id = 1, amount = 50.0))
        every { repository.getTransactions() } returns flowOf(transactions)

        useCase().test {
            assertEquals(transactions, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `invoke propagates repository error`() = runTest {
        every { repository.getTransactions() } returns flow { throw IOException("Network error") }

        useCase().test {
            assertIs<IOException>(awaitError())
        }
    }
}
```

## Тесты Mapper

Mapper — чистые функции, простейший случай:

```kotlin
class TransactionMapperTest {

    @Test
    fun `toDomain maps all fields correctly`() {
        val entity = TransactionEntity(
            id = 1, amount = 100.0, type = "EXPENSE",
            date = 1700000000000L, description = "Coffee",
            accountId = 1L, categoryId = 2L,
        )

        val domain = entity.toDomain()

        assertEquals(1L, domain.id)
        assertEquals(100.0, domain.amount, 0.001)
        assertEquals(TransactionType.EXPENSE, domain.type)
    }

    @Test
    fun `toEntity and toDomain are inverse operations`() {
        val original = Transaction(id = 1, amount = 200.0, type = TransactionType.INCOME)
        val roundTripped = original.toEntity().toDomain()
        assertEquals(original, roundTripped)
    }
}
```

## Turbine API

```kotlin
flow.test {
    val item = awaitItem()              // получить следующий элемент
    awaitComplete()                     // дождаться завершения потока
    awaitError()                        // дождаться ошибки (Flow должен завершиться с ошибкой)
    cancelAndIgnoreRemainingEvents()    // отмена, оставшиеся события игнорируются
    expectNoEvents()                    // убедиться что новых событий нет (за короткое время)
    skipItems(n)                        // пропустить N элементов
}
```

## MockK Cheatsheet

```kotlin
// Mock Flow-возвращающей функции
every { repository.getTransactions() } returns flowOf(listOf(...))

// Mock suspend-функции
coEvery { repository.saveTransaction(any()) } just runs
coEvery { repository.getById(1L) } returns transaction

// Verify вызов suspend-функции
coVerify { repository.saveTransaction(match { it.amount == 100.0 }) }

// Verify функция вызвана ровно N раз
verify(exactly = 1) { useCase.invoke() }

// Spy на реальном объекте
val spy = spyk(RealUseCase(repository))

// Relaxed mock (возвращает дефолтные значения без every{})
val relaxedMock: TransactionRepository = mockk(relaxed = true)
```

## Паттерн AAA (Arrange / Act / Assert)

```kotlin
@Test
fun `save transaction persists to repository`() = runTest {
    // Arrange
    val transaction = Transaction(id = null, amount = 150.0, type = TransactionType.EXPENSE)
    coEvery { repository.saveTransaction(any()) } just runs
    val useCase = SaveTransactionUseCase(repository)

    // Act
    useCase(transaction)

    // Assert
    coVerify { repository.saveTransaction(transaction) }
}
```

## Quality Bar

- Каждый ViewModel: минимум 3 теста — начальное состояние, успешная загрузка, ошибка
- Каждый UseCase: happy path + error case
- Mapper: прямое и обратное преобразование
- НЕ тестируй приватные методы — только публичный state и публичные функции
- Используй `runTest { }` вместо `runBlocking { }` для coroutine-тестов
- Используй `MainDispatcherRule` в каждом ViewModel-тесте

## Anti-patterns

- НЕ используй `Thread.sleep()` — используй Turbine + TestDispatcher
- НЕ используй `runBlocking` в тестах с Flow — используй `runTest`
- НЕ создавай реальные Room-базы в unit-тестах — для Room используй in-memory DB в `androidTest`
- НЕ тестируй один UseCase через другой — мокай зависимости напрямую
- НЕ забывай `cancelAndIgnoreRemainingEvents()` если не читаешь все события из Flow
- НЕ используй `mockk()` без `relaxed = true` для интерфейсов с множеством методов — придётся stub каждый
