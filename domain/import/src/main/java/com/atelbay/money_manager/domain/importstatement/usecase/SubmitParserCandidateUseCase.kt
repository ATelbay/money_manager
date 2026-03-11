package com.atelbay.money_manager.domain.importstatement.usecase

import com.atelbay.money_manager.core.firestore.datasource.FirestoreDataSource
import com.atelbay.money_manager.core.firestore.dto.ParserCandidateDto
import com.atelbay.money_manager.core.parser.RegexValidator
import com.atelbay.money_manager.core.parser.SampleAnonymizer
import com.atelbay.money_manager.core.remoteconfig.ParserConfig
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

class SubmitParserCandidateUseCase @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val sampleAnonymizer: SampleAnonymizer,
    private val regexValidator: RegexValidator,
) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend operator fun invoke(
        config: ParserConfig,
        sampleRows: String,
        userId: String?,
    ) {
        // Step 0: Skip if unauthenticated
        if (userId == null) {
            Timber.d("Skipping candidate submission: user not authenticated")
            return
        }

        // Step 1: Validate regex is ReDoS-safe
        if (!regexValidator.isReDoSSafe(config.transactionPattern)) {
            Timber.w("Skipping candidate submission: regex failed ReDoS check")
            return
        }

        // Step 2: Anonymize sample
        val anonymizedSample = sampleAnonymizer.anonymize(sampleRows)

        // Step 3: Hash userId with HMAC-SHA256
        val userIdHash = hmacHash(userId)

        // Step 4: Serialize config to JSON
        val configJson = json.encodeToString(config)

        // Step 5: Check for existing candidate with same bankId + transactionPattern
        val existing = firestoreDataSource.findParserCandidate(
            bankId = config.bankId,
            transactionPattern = config.transactionPattern,
        )

        if (existing != null) {
            // Step 6: Increment successCount
            Timber.d("Found existing candidate for bank %s, incrementing successCount", config.bankId)
            firestoreDataSource.incrementCandidateSuccessCount(existing.id)
        } else {
            // Step 7: Create new candidate
            Timber.d("Creating new parser candidate for bank %s", config.bankId)
            val now = System.currentTimeMillis()
            val dto = ParserCandidateDto(
                bankId = config.bankId,
                transactionPattern = config.transactionPattern,
                parserConfigJson = configJson,
                anonymizedSample = anonymizedSample,
                userIdHash = userIdHash,
                successCount = 1,
                status = "candidate",
                createdAt = now,
                updatedAt = now,
            )
            firestoreDataSource.pushParserCandidate(dto)
        }
    }

    private fun hmacHash(input: String): String {
        val key = "money_manager_candidate_v1"
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key.toByteArray(), "HmacSHA256"))
        return mac.doFinal(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
