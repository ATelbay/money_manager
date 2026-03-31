package com.atelbay.money_manager.core.firestore.datasource

import com.atelbay.money_manager.core.firestore.dto.AccountDto
import com.atelbay.money_manager.core.firestore.dto.CategoryDto
import com.atelbay.money_manager.core.firestore.dto.ParserCandidateDto
import com.atelbay.money_manager.core.firestore.dto.ParserConfigFirestoreDto
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

    private val parserConfigs
        get() = firestore.collection("parser_configs")

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
            .documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                TransactionDto(
                    remoteId = doc.id,
                    amount = (data["amount"] as? String)
                        ?: (data["amount"] as? Number)?.toDouble()?.toString()
                        ?: "",
                    type = data["type"] as? String ?: "",
                    categoryRemoteId = data["categoryRemoteId"] as? String ?: "",
                    accountRemoteId = data["accountRemoteId"] as? String ?: "",
                    note = data["note"] as? String,
                    date = (data["date"] as? Number)?.toLong() ?: 0,
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0,
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0,
                    isDeleted = data["isDeleted"] as? Boolean ?: false,
                    uniqueHash = data["uniqueHash"] as? String,
                    encryptionVersion = (data["encryptionVersion"] as? Number)?.toInt() ?: 0,
                )
            }

    override suspend fun pullAccounts(userId: String): List<AccountDto> =
        accounts(userId).get().await()
            .documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                AccountDto(
                    remoteId = doc.id,
                    name = data["name"] as? String ?: "",
                    currency = data["currency"] as? String ?: "",
                    balance = (data["balance"] as? String)
                        ?: (data["balance"] as? Number)?.toDouble()?.toString()
                        ?: "",
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0,
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0,
                    isDeleted = data["isDeleted"] as? Boolean ?: false,
                    encryptionVersion = (data["encryptionVersion"] as? Number)?.toInt() ?: 0,
                )
            }

    override suspend fun pullCategories(userId: String): List<CategoryDto> =
        categories(userId).get().await()
            .documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                CategoryDto(
                    remoteId = doc.id,
                    name = data["name"] as? String ?: "",
                    icon = data["icon"] as? String ?: "",
                    color = (data["color"] as? String)
                        ?: (data["color"] as? Number)?.toLong()?.toString()
                        ?: "",
                    type = data["type"] as? String ?: "",
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0,
                    isDeleted = data["isDeleted"] as? Boolean ?: false,
                    encryptionVersion = (data["encryptionVersion"] as? Number)?.toInt() ?: 0,
                )
            }

    override suspend fun findParserCandidate(
        bankId: String,
        transactionPattern: String,
    ): ParserCandidateDto? {
        val snapshot = parserCandidates
            .whereEqualTo("bankId", bankId)
            .whereEqualTo("transactionPattern", transactionPattern)
            .whereEqualTo("status", "candidate")
            .limit(1)
            .get()
            .await()
        return snapshot.documents.firstOrNull()?.toObject(ParserCandidateDto::class.java)
    }

    override suspend fun findTableParserCandidate(bankId: String): ParserCandidateDto? {
        val snapshot = parserCandidates
            .whereEqualTo("bankId", bankId)
            .whereEqualTo("configType", "table")
            .whereEqualTo("status", "candidate")
            .limit(1)
            .get()
            .await()
        return snapshot.documents.firstOrNull()?.toObject(ParserCandidateDto::class.java)
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

    override suspend fun findCandidatesByUser(
        userIdHash: String,
        configType: String,
    ): List<ParserCandidateDto> {
        val snapshot = parserCandidates
            .whereEqualTo("userIdHash", userIdHash)
            .whereEqualTo("configType", configType)
            .orderBy("updatedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.toObject(ParserCandidateDto::class.java) }
    }

    override suspend fun pullActiveParserConfigs(): List<ParserConfigFirestoreDto> {
        return try {
            parserConfigs
                .whereEqualTo("status", "active")
                .orderBy("updatedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(ParserConfigFirestoreDto::class.java)
        } catch (e: Exception) {
            Timber.w(e, "Failed to pull active parser configs from Firestore")
            emptyList()
        }
    }

    override suspend fun getParserConfigsVersion(): Long? {
        return try {
            val doc = firestore.collection("parser_configs_meta")
                .document("latest")
                .get()
                .await()
            (doc.data?.get("globalVersion") as? Number)?.toLong()
        } catch (e: Exception) {
            Timber.w(e, "Failed to get parser configs version from Firestore")
            null
        }
    }
}
