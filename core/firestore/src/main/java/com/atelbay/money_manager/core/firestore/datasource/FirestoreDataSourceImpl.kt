package com.atelbay.money_manager.core.firestore.datasource

import com.atelbay.money_manager.core.firestore.dto.AccountDto
import com.atelbay.money_manager.core.firestore.dto.CategoryDto
import com.atelbay.money_manager.core.firestore.dto.TransactionDto
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
}
