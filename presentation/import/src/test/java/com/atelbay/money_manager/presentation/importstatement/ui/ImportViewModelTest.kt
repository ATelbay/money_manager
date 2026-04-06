package com.atelbay.money_manager.presentation.importstatement.ui

import com.atelbay.money_manager.core.auth.AuthUser
import com.atelbay.money_manager.core.datastore.UserPreferences
import com.atelbay.money_manager.core.ui.theme.AppStrings
import com.atelbay.money_manager.core.ui.theme.RussianStrings
import com.atelbay.money_manager.core.model.Account
import com.atelbay.money_manager.core.model.ImportResult
import com.atelbay.money_manager.core.model.ImportState
import com.atelbay.money_manager.core.model.ParsedTransaction
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.core.remoteconfig.RegexParserProfile
import com.atelbay.money_manager.domain.accounts.usecase.GetAccountsUseCase
import com.atelbay.money_manager.domain.auth.repository.AuthRepository
import com.atelbay.money_manager.domain.categories.usecase.GetCategoriesUseCase
import com.atelbay.money_manager.domain.importstatement.usecase.ImportTransactionsUseCase
import com.atelbay.money_manager.domain.importstatement.usecase.ParseResult
import com.atelbay.money_manager.domain.importstatement.usecase.ParseStatementUseCase
import com.atelbay.money_manager.domain.importstatement.usecase.SubmitParserCandidateUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class ImportViewModelTest {

    private val testStrings: AppStrings = RussianStrings

    class MainDispatcherRule(
        private val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
    ) : TestWatcher() {
        override fun starting(description: Description) {
            Dispatchers.setMain(dispatcher)
        }

        override fun finished(description: Description) {
            Dispatchers.resetMain()
        }
    }

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var parseStatementUseCase: ParseStatementUseCase
    private lateinit var importTransactionsUseCase: ImportTransactionsUseCase
    private lateinit var userPreferences: UserPreferences
    private lateinit var getCategoriesUseCase: GetCategoriesUseCase
    private lateinit var getAccountsUseCase: GetAccountsUseCase
    private lateinit var submitParserCandidateUseCase: SubmitParserCandidateUseCase
    private lateinit var authRepository: AuthRepository

    private val testAccount = Account(id = 1L, name = "Test", balance = 0.0, currency = "KZT")
    private val testTransaction = ParsedTransaction(
        date = LocalDate(2024, 1, 15),
        amount = 5000.0,
        type = TransactionType.EXPENSE,
        operationType = "Purchase",
        details = "Test purchase",
        categoryId = 1L,
        suggestedCategoryName = null,
        confidence = 0.9f,
        needsReview = false,
        uniqueHash = "hash123",
    )
    private val testImportResult = ImportResult(
        total = 1,
        newTransactions = listOf(testTransaction),
        duplicates = 0,
        errors = emptyList(),
    )
    private val testAiConfig = RegexParserProfile(
        bankId = "ai_bank",
        bankMarkers = listOf("AI Bank"),
        transactionPattern = "\\d+",
        dateFormat = "dd.MM.yyyy",
        operationTypeMap = mapOf("Purchase" to "expense"),
    )

    @Before
    fun setUp() {
        parseStatementUseCase = mockk()
        importTransactionsUseCase = mockk()
        userPreferences = mockk()
        getCategoriesUseCase = mockk()
        getAccountsUseCase = mockk()
        submitParserCandidateUseCase = mockk(relaxUnitFun = true)
        authRepository = mockk()

        every { getAccountsUseCase() } returns flowOf(listOf(testAccount))
        every { userPreferences.selectedAccountId } returns flowOf(1L)
        every { getCategoriesUseCase(TransactionType.EXPENSE) } returns flowOf(emptyList())
        every { getCategoriesUseCase(TransactionType.INCOME) } returns flowOf(emptyList())
        every { authRepository.observeCurrentUser() } returns flowOf(
            AuthUser("uid", null, null, null),
        )
    }

    private fun createViewModel(): ImportViewModel = ImportViewModel(
        parseStatementUseCase = parseStatementUseCase,
        importTransactionsUseCase = importTransactionsUseCase,
        userPreferences = userPreferences,
        getCategoriesUseCase = getCategoriesUseCase,
        getAccountsUseCase = getAccountsUseCase,
        submitParserCandidateUseCase = submitParserCandidateUseCase,
        authRepository = authRepository,
    )

    @Test
    fun `initial state is Idle`() = runTest {
        val viewModel = createViewModel()
        assertEquals(ImportState.Idle, viewModel.state.value)
    }

    @Test
    fun `after successful import with AI config, submitParserCandidateUseCase is called`() = runTest {
        val parseResult = ParseResult(
            importResult = testImportResult,
            aiGeneratedConfig = testAiConfig,
            sampleRows = "sample row data",
        )
        coEvery { parseStatementUseCase(any(), any()) } returns parseResult
        coEvery { importTransactionsUseCase(any(), any(), any()) } returns 1

        val viewModel = createViewModel()

        // Trigger parse
        viewModel.onPdfSelected(byteArrayOf(1), testStrings)
        advanceUntilIdle()

        // Should be in Preview state
        assertTrue(viewModel.state.value is ImportState.Preview)

        // Trigger import
        viewModel.importTransactions(testStrings)
        advanceUntilIdle()

        // Should be in Success state
        assertTrue(viewModel.state.value is ImportState.Success)

        // Verify submitParserCandidateUseCase was called
        coVerify(exactly = 1) {
            submitParserCandidateUseCase(testAiConfig, "sample row data", "uid")
        }
    }

    @Test
    fun `after successful import with regex config, submission is NOT called`() = runTest {
        val parseResult = ParseResult(
            importResult = testImportResult,
            aiGeneratedConfig = null,
            sampleRows = null,
        )
        coEvery { parseStatementUseCase(any(), any()) } returns parseResult
        coEvery { importTransactionsUseCase(any(), any(), any()) } returns 1

        val viewModel = createViewModel()

        // Trigger parse
        viewModel.onPdfSelected(byteArrayOf(1), testStrings)
        advanceUntilIdle()

        // Trigger import
        viewModel.importTransactions(testStrings)
        advanceUntilIdle()

        assertTrue(viewModel.state.value is ImportState.Success)

        // Verify submitParserCandidateUseCase was NOT called
        coVerify(exactly = 0) { submitParserCandidateUseCase(any(), any(), any()) }
    }

    @Test
    fun `submission failure does not affect import success state`() = runTest {
        val parseResult = ParseResult(
            importResult = testImportResult,
            aiGeneratedConfig = testAiConfig,
            sampleRows = "sample row data",
        )
        coEvery { parseStatementUseCase(any(), any()) } returns parseResult
        coEvery { importTransactionsUseCase(any(), any(), any()) } returns 1
        coEvery {
            submitParserCandidateUseCase(any(), any(), any())
        } throws RuntimeException("Firestore unavailable")

        val viewModel = createViewModel()

        // Trigger parse
        viewModel.onPdfSelected(byteArrayOf(1), testStrings)
        advanceUntilIdle()

        // Trigger import
        viewModel.importTransactions(testStrings)
        advanceUntilIdle()

        // State should still be Success despite submission failure
        val state = viewModel.state.value
        assertTrue("Expected Success but was $state", state is ImportState.Success)
        assertEquals(1, (state as ImportState.Success).imported)
    }
}
