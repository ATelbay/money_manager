package com.atelbay.money_manager.core.firestore.datasource

import com.atelbay.money_manager.core.firestore.dto.AccountDto
import com.atelbay.money_manager.core.firestore.dto.BudgetDto
import com.atelbay.money_manager.core.firestore.dto.CategoryDto
import com.atelbay.money_manager.core.firestore.dto.DebtDto
import com.atelbay.money_manager.core.firestore.dto.DebtPaymentDto
import com.atelbay.money_manager.core.firestore.dto.ParserCandidateDto
import com.atelbay.money_manager.core.firestore.dto.RecurringTransactionDto
import com.atelbay.money_manager.core.firestore.dto.RegexParserProfileFirestoreDto
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

    private fun budgets(userId: String) =
        firestore.collection("users").document(userId).collection("budgets")

    private fun recurringTransactions(userId: String) =
        firestore.collection("users").document(userId).collection("recurring_transactions")

    private fun debts(userId: String) =
        firestore.collection("users").document(userId).collection("debts")

    private fun debtPayments(userId: String) =
        firestore.collection("users").document(userId).collection("debt_payments")

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

    override suspend fun pushBudget(userId: String, dto: BudgetDto) {
        try {
            budgets(userId).document(dto.remoteId).set(dto).await()
        } catch (e: Exception) {
            Timber.e(e, "Failed to push budget ${dto.remoteId}")
            throw e
        }
    }

    override suspend fun pushRecurringTransaction(userId: String, dto: RecurringTransactionDto) {
        try {
            recurringTransactions(userId).document(dto.remoteId).set(dto).await()
        } catch (e: Exception) {
            Timber.e(e, "Failed to push recurring transaction ${dto.remoteId}")
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

    override suspend fun pullBudgets(userId: String): List<BudgetDto> =
        budgets(userId).get().await()
            .documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                BudgetDto(
                    remoteId = doc.id,
                    categoryRemoteId = data["categoryRemoteId"] as? String ?: "",
                    monthlyLimit = (data["monthlyLimit"] as? String)
                        ?: (data["monthlyLimit"] as? Number)?.toDouble()?.toString()
                        ?: "",
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0,
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0,
                    isDeleted = data["isDeleted"] as? Boolean ?: false,
                    encryptionVersion = (data["encryptionVersion"] as? Number)?.toInt() ?: 0,
                )
            }

    override suspend fun pullRecurringTransactions(userId: String): List<RecurringTransactionDto> =
        recurringTransactions(userId).get().await()
            .documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                RecurringTransactionDto(
                    remoteId = doc.id,
                    amount = (data["amount"] as? String)
                        ?: (data["amount"] as? Number)?.toDouble()?.toString()
                        ?: "",
                    type = data["type"] as? String ?: "",
                    categoryRemoteId = data["categoryRemoteId"] as? String ?: "",
                    accountRemoteId = data["accountRemoteId"] as? String ?: "",
                    note = data["note"] as? String,
                    frequency = data["frequency"] as? String ?: "",
                    startDate = (data["startDate"] as? Number)?.toLong() ?: 0,
                    endDate = (data["endDate"] as? Number)?.toLong(),
                    dayOfMonth = (data["dayOfMonth"] as? Number)?.toInt(),
                    dayOfWeek = (data["dayOfWeek"] as? Number)?.toInt(),
                    lastGeneratedDate = (data["lastGeneratedDate"] as? Number)?.toLong(),
                    isActive = data["isActive"] as? Boolean ?: true,
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0,
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0,
                    isDeleted = data["isDeleted"] as? Boolean ?: false,
                    encryptionVersion = (data["encryptionVersion"] as? Number)?.toInt() ?: 0,
                )
            }

    override suspend fun pushDebt(userId: String, dto: DebtDto) {
        try {
            debts(userId).document(dto.remoteId).set(dto).await()
        } catch (e: Exception) {
            Timber.e(e, "Failed to push debt ${dto.remoteId}")
            throw e
        }
    }

    override suspend fun pullDebts(userId: String): List<DebtDto> =
        debts(userId).get().await()
            .documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                DebtDto(
                    remoteId = doc.id,
                    contactName = data["contactName"] as? String ?: "",
                    direction = data["direction"] as? String ?: "",
                    totalAmount = (data["totalAmount"] as? String)
                        ?: (data["totalAmount"] as? Number)?.toDouble()?.toString()
                        ?: "",
                    currency = data["currency"] as? String ?: "",
                    accountRemoteId = data["accountRemoteId"] as? String ?: "",
                    note = data["note"] as? String ?: "",
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0,
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0,
                    isDeleted = data["isDeleted"] as? Boolean ?: false,
                    encryptionVersion = (data["encryptionVersion"] as? Number)?.toInt() ?: 0,
                )
            }

    override suspend fun deleteDebt(userId: String, remoteId: String) {
        try {
            debts(userId).document(remoteId).delete().await()
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete debt $remoteId")
            throw e
        }
    }

    override suspend fun pushDebtPayment(userId: String, dto: DebtPaymentDto) {
        try {
            debtPayments(userId).document(dto.remoteId).set(dto).await()
        } catch (e: Exception) {
            Timber.e(e, "Failed to push debt payment ${dto.remoteId}")
            throw e
        }
    }

    override suspend fun pullDebtPayments(userId: String): List<DebtPaymentDto> =
        debtPayments(userId).get().await()
            .documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                DebtPaymentDto(
                    remoteId = doc.id,
                    debtRemoteId = data["debtRemoteId"] as? String ?: "",
                    amount = (data["amount"] as? String)
                        ?: (data["amount"] as? Number)?.toDouble()?.toString()
                        ?: "",
                    date = (data["date"] as? Number)?.toLong() ?: 0,
                    note = data["note"] as? String ?: "",
                    transactionRemoteId = data["transactionRemoteId"] as? String ?: "",
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0,
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0,
                    isDeleted = data["isDeleted"] as? Boolean ?: false,
                    encryptionVersion = (data["encryptionVersion"] as? Number)?.toInt() ?: 0,
                )
            }

    override suspend fun deleteDebtPayment(userId: String, remoteId: String) {
        try {
            debtPayments(userId).document(remoteId).delete().await()
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete debt payment $remoteId")
            throw e
        }
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

    override suspend fun pullActiveParserConfigs(): List<RegexParserProfileFirestoreDto> {
        return try {
            parserConfigs
                .whereEqualTo("status", "active")
                .orderBy("updatedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(RegexParserProfileFirestoreDto::class.java)
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
