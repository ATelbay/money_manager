package com.atelbay.money_manager.domain.importstatement.usecase

import com.atelbay.money_manager.core.firestore.datasource.FirestoreDataSource
import com.atelbay.money_manager.core.firestore.dto.ParserCandidateDto
import com.atelbay.money_manager.core.model.TableParserProfile
import com.atelbay.money_manager.core.parser.RegexValidator
import com.atelbay.money_manager.core.parser.SampleAnonymizer
import com.atelbay.money_manager.core.remoteconfig.RegexParserProfile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

class SubmitParserCandidateUseCase @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val sampleAnonymizer: SampleAnonymizer,
    private val regexValidator: RegexValidator,
    private val userIdHasher: UserIdHasher,
) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend operator fun invoke(
        config: RegexParserProfile,
        sampleRows: String,
        userId: String?,
    ) {
        // Step 1: Validate regex is ReDoS-safe
        if (!regexValidator.isReDoSSafe(config.transactionPattern)) {
            Timber.w("Skipping candidate submission: regex failed ReDoS check")
            return
        }

        val anonymizedSample = sampleAnonymizer.anonymize(sampleRows)
        val configJson = json.encodeToString(config)
        submitCandidate(
            bankId = config.bankId,
            configJson = configJson,
            anonymizedSample = anonymizedSample,
            userId = userId,
            configType = "regex",
            transactionPattern = config.transactionPattern,
        )
    }

    suspend fun submitTableConfig(
        config: TableParserProfile,
        sampleTableRows: List<List<String>>,
        userId: String?,
    ) {
        val anonymizedSample = sampleAnonymizer.anonymize(
            sampleTableRows.joinToString("\n") { it.joinToString(" | ") },
        )
        val configJson = json.encodeToString(config)
        submitCandidate(
            bankId = config.bankId,
            configJson = configJson,
            anonymizedSample = anonymizedSample,
            userId = userId,
            configType = "table",
            transactionPattern = "",
        )
    }

    private suspend fun submitCandidate(
        bankId: String,
        configJson: String,
        anonymizedSample: String,
        userId: String?,
        configType: String,
        transactionPattern: String,
    ) {
        val userIdHash = userIdHasher.computeHash(userId)

        val existing = if (configType == "table") {
            firestoreDataSource.findTableParserCandidate(bankId = bankId)
        } else {
            firestoreDataSource.findParserCandidate(
                bankId = bankId,
                transactionPattern = transactionPattern,
            )
        }

        if (existing != null) {
            Timber.d("Found existing candidate for bank %s, incrementing successCount", bankId)
            firestoreDataSource.incrementCandidateSuccessCount(existing.id)
        } else {
            Timber.d("Creating new parser candidate for bank %s", bankId)
            val now = System.currentTimeMillis()
            val dto = ParserCandidateDto(
                bankId = bankId,
                transactionPattern = transactionPattern,
                parserConfigJson = configJson,
                anonymizedSample = anonymizedSample,
                userIdHash = userIdHash,
                successCount = 1,
                status = "candidate",
                createdAt = now,
                updatedAt = now,
                configType = configType,
            )
            firestoreDataSource.pushParserCandidate(dto)
        }
    }
}
