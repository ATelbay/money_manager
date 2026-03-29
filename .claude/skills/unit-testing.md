---
description: "Unit testing in Money Manager: ViewModel tests, UseCase tests, MockK for mocking, Turbine for Flow, MainDispatcherRule, test placement per module"
---

# Unit Testing

## Context

Money Manager is the foundation for a master's thesis on UI automation, so test coverage is critical. This skill covers **unit tests** (JVM, no Android), as distinct from **UI tests** (instrumented, `androidTest/`).

**Dependencies (from `libs.versions.toml`):**
```toml
turbine = "1.2.1"        # Flow testing
mockk = "1.14.9"         # Kotlin mocking
coroutines = "1.10.2"    # includes kotlinx-coroutines-test
junit = "4.13.2"
```

## Test Placement

| Test type | Directory | Run with |
|-----------|-----------|----------|
| Unit (ViewModel, UseCase, Repository) | `{module}/src/test/` | `./gradlew test` (JVM) |
| UI/Instrumented | `presentation/*/src/androidTest/` | `./gradlew connectedAndroidTest` |

Examples:
- `domain/transactions/src/test/` — UseCase tests
- `data/transactions/src/test/` — mapper tests, RepositoryImpl tests (with mocked DAO)
- `presentation/transactions/src/test/` — ViewModel tests

**build.gradle.kts** (add to the relevant module):
```kotlin
dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
```

## MainDispatcherRule

Required for ViewModel tests — replaces `Dispatchers.Main` with a test dispatcher.

```kotlin
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(testDispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}
```

## ViewModel Tests

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

## UseCase Tests

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

## Mapper Tests

Mappers are pure functions — the simplest case:

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
    val item = awaitItem()              // get the next emitted item
    awaitComplete()                     // wait for the flow to complete
    awaitError()                        // wait for an error (flow must terminate with an error)
    cancelAndIgnoreRemainingEvents()    // cancel and ignore any remaining events
    expectNoEvents()                    // assert no new events arrive (within a short time)
    skipItems(n)                        // skip N items
}
```

## MockK Cheatsheet

```kotlin
// Mock a Flow-returning function
every { repository.getTransactions() } returns flowOf(listOf(...))

// Mock a suspend function
coEvery { repository.saveTransaction(any()) } just runs
coEvery { repository.getById(1L) } returns transaction

// Verify a suspend function call
coVerify { repository.saveTransaction(match { it.amount == 100.0 }) }

// Verify a function was called exactly N times
verify(exactly = 1) { useCase.invoke() }

// Spy on a real object
val spy = spyk(RealUseCase(repository))

// Relaxed mock (returns default values without every{})
val relaxedMock: TransactionRepository = mockk(relaxed = true)
```

## AAA Pattern (Arrange / Act / Assert)

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

- Each ViewModel: minimum 3 tests — initial state, successful load, error
- Each UseCase: happy path + error case
- Mapper: forward and inverse transformation
- Do NOT test private methods — only public state and public functions
- Use `runTest { }` instead of `runBlocking { }` for coroutine tests
- Use `MainDispatcherRule` in every ViewModel test

## Anti-patterns

- Do NOT use `Thread.sleep()` — use Turbine + TestDispatcher instead
- Do NOT use `runBlocking` in tests with Flow — use `runTest`
- Do NOT create real Room databases in unit tests — use in-memory DB in `androidTest` for Room
- Do NOT test one UseCase through another — mock dependencies directly
- Do NOT forget `cancelAndIgnoreRemainingEvents()` if you don't consume all events from a Flow
- Do NOT use `mockk()` without `relaxed = true` for interfaces with many methods — you'll have to stub every one
