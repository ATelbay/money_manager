package com.atelbay.money_manager.core.firestore.datasource

import com.atelbay.money_manager.core.firestore.dto.AccountDto
import com.atelbay.money_manager.core.firestore.dto.CategoryDto
import com.atelbay.money_manager.core.firestore.dto.ParserCandidateDto
import com.atelbay.money_manager.core.firestore.dto.TransactionDto
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : FirestoreDataSource {

    private fun transactions(userId: String) =
        firestore.collection("users").document(userId).collection("transactions")

    private fun accounts(userId: String) =
        firestore.collection("users").document(userId).collection("accounts")

    private fun categories(userId: String) =
        firestore.collection("users").document(userId).collection("categories")

    private val parserCandidates
        get() = firestore.collection("parser_candidates")

    override suspend fun pushTransaction(userId: String, dto: TransactionDto) {
        try {
            transactions(userId).document(dto.remoteId).set(dto).await()
        } catch (e: Exception) {
            Timber.e(e, "Failed to push transaction ${dto.remoteId}")
            throw e
        }
    }

    override suspend fun pushAccount(userId: String, dto: AccountDto) {
        try {
            accounts(userId).document(dto.remoteId).set(dto).await()
        } catch (e: Exception) {
            Timber.e(e, "Failed to push account ${dto.remoteId}")
            throw e
        }
    }

    override suspend fun pushCategory(userId: String, dto: CategoryDto) {
        try {
            categories(userId).document(dto.remoteId).set(dto).await()
        } catch (e: Exception) {
            Timber.e(e, "Failed to push category ${dto.remoteId}")
            throw e
        }
    }

    override suspend fun pullTransactions(userId: String): List<TransactionDto> =
        transactions(userId).get().await()
            .documents.mapNotNull { it.toObject(TransactionDto::class.java) }

    override suspend fun pullAccounts(userId: String): List<AccountDto> =
        accounts(userId).get().await()
            .documents.mapNotNull { it.toObject(AccountDto::class.java) }

    override suspend fun pullCategories(userId: String): List<CategoryDto> =
        categories(userId).get().await()
            .documents.mapNotNull { it.toObject(CategoryDto::class.java) }

    override suspend fun findParserCandidate(
        bankId: String,
        transactionPattern: String,
    ): ParserCandidateDto? {
        return try {
            val snapshot = parserCandidates
                .whereEqualTo("bankId", bankId)
                .whereEqualTo("transactionPattern", transactionPattern)
                .whereEqualTo("status", "candidate")
                .limit(1)
                .get()
                .await()
            snapshot.documents.firstOrNull()?.toObject(ParserCandidateDto::class.java)
        } catch (e: Exception) {
            Timber.e(e, "Failed to find parser candidate for bank %s", bankId)
            null
        }
    }

    override suspend fun pushParserCandidate(dto: ParserCandidateDto) {
        try {
            parserCandidates.add(dto).await()
        } catch (e: Exception) {
            Timber.e(e, "Failed to push parser candidate for bank %s", dto.bankId)
            throw e
        }
    }

    override suspend fun incrementCandidateSuccessCount(candidateId: String) {
        try {
            parserCandidates.document(candidateId).update(
                mapOf(
                    "successCount" to FieldValue.increment(1),
                    "updatedAt" to System.currentTimeMillis(),
                )
            ).await()
        } catch (e: Exception) {
            Timber.e(e, "Failed to increment success count for candidate %s", candidateId)
            throw e
        }
    }
}
