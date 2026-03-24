package com.atelbay.money_manager.domain.importstatement.usecase

import com.atelbay.money_manager.core.datastore.UserPreferences
import com.atelbay.money_manager.core.firestore.datasource.FirestoreDataSource
import com.atelbay.money_manager.core.firestore.dto.ParserCandidateDto
import com.atelbay.money_manager.core.model.TableParserConfig
import com.atelbay.money_manager.core.parser.RegexValidator
import com.atelbay.money_manager.core.parser.SampleAnonymizer
import com.atelbay.money_manager.core.remoteconfig.ParserConfig
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SubmitParserCandidateUseCaseTest {

    private lateinit var firestoreDataSource: FirestoreDataSource
    private lateinit var sampleAnonymizer: SampleAnonymizer
    private lateinit var regexValidator: RegexValidator
    private lateinit var userPreferences: UserPreferences
    private lateinit var useCase: SubmitParserCandidateUseCase

    private val testConfig = ParserConfig(
        bankId = "test_bank",
        bankMarkers = listOf("Test Bank"),
        transactionPattern = "\\d+",
        dateFormat = "dd.MM.yyyy",
        operationTypeMap = mapOf("Purchase" to "expense"),
    )

    @Before
    fun setUp() {
        firestoreDataSource = mockk(relaxUnitFun = true)
        sampleAnonymizer = mockk()
        regexValidator = mockk()
        userPreferences = mockk()
        coEvery { userPreferences.getOrCreateAnonymousDeviceId() } returns "device-uuid-123"
        useCase = SubmitParserCandidateUseCase(firestoreDataSource, sampleAnonymizer, regexValidator, userPreferences)
    }

    @Test
    fun `new candidate is created when no existing match found`() = runTest {
        every { regexValidator.isReDoSSafe(testConfig.transactionPattern) } returns true
        every { sampleAnonymizer.anonymize("sample row") } returns "anonymized row"
        coEvery {
            firestoreDataSource.findParserCandidate(
                bankId = testConfig.bankId,
                transactionPattern = testConfig.transactionPattern,
            )
        } returns null

        useCase(testConfig, "sample row", "user123")

        coVerify(exactly = 1) {
            firestoreDataSource.pushParserCandidate(
                match { dto ->
                    dto.bankId == "test_bank" &&
                        dto.transactionPattern == "\\d+" &&
                        dto.anonymizedSample == "anonymized row" &&
                        dto.successCount == 1 &&
                        dto.status == "candidate"
                },
            )
        }
        coVerify(exactly = 0) { firestoreDataSource.incrementCandidateSuccessCount(any()) }
    }

    @Test
    fun `successCount is incremented when existing match found`() = runTest {
        every { regexValidator.isReDoSSafe(testConfig.transactionPattern) } returns true
        every { sampleAnonymizer.anonymize("sample row") } returns "anonymized row"
        val existingDto = ParserCandidateDto(
            id = "existing_id",
            bankId = "test_bank",
            transactionPattern = "\\d+",
            successCount = 5,
        )
        coEvery {
            firestoreDataSource.findParserCandidate(
                bankId = testConfig.bankId,
                transactionPattern = testConfig.transactionPattern,
            )
        } returns existingDto

        useCase(testConfig, "sample row", "user123")

        coVerify(exactly = 1) { firestoreDataSource.incrementCandidateSuccessCount("existing_id") }
        coVerify(exactly = 0) { firestoreDataSource.pushParserCandidate(any()) }
    }

    @Test
    fun `skipped when regex is ReDoS unsafe`() = runTest {
        every { regexValidator.isReDoSSafe(testConfig.transactionPattern) } returns false

        useCase(testConfig, "sample row", "user123")

        coVerify(exactly = 0) { firestoreDataSource.pushParserCandidate(any()) }
        coVerify(exactly = 0) { firestoreDataSource.incrementCandidateSuccessCount(any()) }
        coVerify(exactly = 0) { firestoreDataSource.findParserCandidate(any(), any()) }
    }

    @Test
    fun `userId is hashed with HMAC-SHA256 before submission`() = runTest {
        every { regexValidator.isReDoSSafe(testConfig.transactionPattern) } returns true
        every { sampleAnonymizer.anonymize(any()) } returns "anonymized"
        coEvery {
            firestoreDataSource.findParserCandidate(any(), any())
        } returns null

        useCase(testConfig, "sample", "user123")

        // Compute plain SHA-256 to ensure HMAC produces a different result
        val plainSha256 = java.security.MessageDigest.getInstance("SHA-256")
            .digest("user123".toByteArray())
            .joinToString("") { "%02x".format(it) }

        coVerify {
            firestoreDataSource.pushParserCandidate(
                match { dto ->
                    // Must be a valid 64-character hex string
                    dto.userIdHash.length == 64 &&
                        dto.userIdHash.all { it in '0'..'9' || it in 'a'..'f' } &&
                        dto.userIdHash != "user123" &&
                        // Must NOT be plain SHA-256 (HMAC uses a keyed hash)
                        dto.userIdHash != plainSha256
                },
            )
        }
    }

    @Test
    fun `sample is anonymized before submission`() = runTest {
        val rawSample = "01.01.2024 John Doe 50000 KZT"
        val anonymizedSample = "01.01.2024 MERCHANT_1 50000 KZT"

        every { regexValidator.isReDoSSafe(testConfig.transactionPattern) } returns true
        every { sampleAnonymizer.anonymize(rawSample) } returns anonymizedSample
        coEvery {
            firestoreDataSource.findParserCandidate(any(), any())
        } returns null

        useCase(testConfig, rawSample, "user123")

        coVerify {
            firestoreDataSource.pushParserCandidate(
                match { dto -> dto.anonymizedSample == anonymizedSample },
            )
        }
    }

    @Test
    fun `uses anonymous device ID when userId is null`() = runTest {
        every { regexValidator.isReDoSSafe(testConfig.transactionPattern) } returns true
        every { sampleAnonymizer.anonymize("sample row") } returns "anonymized row"
        coEvery {
            firestoreDataSource.findParserCandidate(any(), any())
        } returns null

        useCase(testConfig, "sample row", null)

        coVerify(exactly = 1) { userPreferences.getOrCreateAnonymousDeviceId() }
        coVerify(exactly = 1) {
            firestoreDataSource.pushParserCandidate(
                match { dto ->
                    dto.userIdHash.length == 64 &&
                        dto.userIdHash.all { it in '0'..'9' || it in 'a'..'f' }
                },
            )
        }
    }

    // ==================== submitTableConfig ====================

    private val testTableConfig = TableParserConfig(
        bankId = "bcc",
        bankMarkers = listOf("BCC"),
        dateColumn = 0,
        amountColumn = 1,
        dateFormat = "dd.MM.yyyy",
        amountFormat = "space_dot",
    )

    private val testTableRows = listOf(
        listOf("25.03.2026", "107 061.00", "Purchase", "Store"),
        listOf("26.03.2026", "5 000.00", "ATM", "Cash"),
    )

    @Test
    fun `submitTableConfig creates new candidate when no existing match`() = runTest {
        every { sampleAnonymizer.anonymize(any()) } returns "anonymized table"
        coEvery { firestoreDataSource.findTableParserCandidate(bankId = "bcc") } returns null

        useCase.submitTableConfig(testTableConfig, testTableRows, "user123")

        coVerify(exactly = 1) {
            firestoreDataSource.pushParserCandidate(
                match { dto ->
                    dto.bankId == "bcc" &&
                        dto.configType == "table" &&
                        dto.transactionPattern == "" &&
                        dto.anonymizedSample == "anonymized table" &&
                        dto.successCount == 1 &&
                        dto.status == "candidate"
                },
            )
        }
        coVerify(exactly = 0) { firestoreDataSource.incrementCandidateSuccessCount(any()) }
    }

    @Test
    fun `submitTableConfig increments when existing table candidate found`() = runTest {
        every { sampleAnonymizer.anonymize(any()) } returns "anonymized table"
        val existingDto = ParserCandidateDto(
            id = "existing_table_id",
            bankId = "bcc",
            transactionPattern = "",
            successCount = 3,
            configType = "table",
        )
        coEvery { firestoreDataSource.findTableParserCandidate(bankId = "bcc") } returns existingDto

        useCase.submitTableConfig(testTableConfig, testTableRows, "user123")

        coVerify(exactly = 1) { firestoreDataSource.incrementCandidateSuccessCount("existing_table_id") }
        coVerify(exactly = 0) { firestoreDataSource.pushParserCandidate(any()) }
    }

    @Test
    fun `submitTableConfig uses anonymous device ID when userId is null`() = runTest {
        every { sampleAnonymizer.anonymize(any()) } returns "anonymized table"
        coEvery { firestoreDataSource.findTableParserCandidate(bankId = "bcc") } returns null

        useCase.submitTableConfig(testTableConfig, testTableRows, null)

        coVerify(exactly = 1) { userPreferences.getOrCreateAnonymousDeviceId() }
        coVerify(exactly = 1) {
            firestoreDataSource.pushParserCandidate(
                match { dto ->
                    dto.userIdHash.length == 64 &&
                        dto.userIdHash.all { it in '0'..'9' || it in 'a'..'f' }
                },
            )
        }
    }

    @Test
    fun `submitTableConfig uses findTableParserCandidate not findParserCandidate`() = runTest {
        every { sampleAnonymizer.anonymize(any()) } returns "anonymized table"
        coEvery { firestoreDataSource.findTableParserCandidate(bankId = "bcc") } returns null

        useCase.submitTableConfig(testTableConfig, testTableRows, "user123")

        coVerify(exactly = 1) { firestoreDataSource.findTableParserCandidate(bankId = "bcc") }
        coVerify(exactly = 0) { firestoreDataSource.findParserCandidate(any(), any()) }
    }

    @Test
    fun `submitTableConfig anonymizes table rows as pipe-separated lines`() = runTest {
        every { sampleAnonymizer.anonymize(any()) } returns "anonymized"
        coEvery { firestoreDataSource.findTableParserCandidate(bankId = "bcc") } returns null

        useCase.submitTableConfig(testTableConfig, testTableRows, "user123")

        // Verify the raw input to anonymizer is pipe-separated
        val expectedRaw = "25.03.2026 | 107 061.00 | Purchase | Store\n26.03.2026 | 5 000.00 | ATM | Cash"
        coVerify { sampleAnonymizer.anonymize(expectedRaw) }
    }
}
