package com.atelbay.money_manager.core.firestore.datasource

import com.atelbay.money_manager.core.firestore.dto.AccountDto
import com.atelbay.money_manager.core.firestore.dto.CategoryDto
import com.atelbay.money_manager.core.firestore.dto.ParserCandidateDto
import com.atelbay.money_manager.core.firestore.dto.ParserConfigFirestoreDto
import com.atelbay.money_manager.core.firestore.dto.TransactionDto

interface FirestoreDataSource {

    suspend fun pushTransaction(userId: String, dto: TransactionDto)
    suspend fun pushAccount(userId: String, dto: AccountDto)
    suspend fun pushCategory(userId: String, dto: CategoryDto)

    suspend fun pullTransactions(userId: String): List<TransactionDto>
    suspend fun pullAccounts(userId: String): List<AccountDto>
    suspend fun pullCategories(userId: String): List<CategoryDto>

    suspend fun findParserCandidate(bankId: String, transactionPattern: String): ParserCandidateDto?
    suspend fun findTableParserCandidate(bankId: String): ParserCandidateDto?
    suspend fun pushParserCandidate(dto: ParserCandidateDto)
    suspend fun incrementCandidateSuccessCount(candidateId: String)
    suspend fun findCandidatesByUser(userIdHash: String, configType: String): List<ParserCandidateDto>

    suspend fun pullActiveParserConfigs(): List<ParserConfigFirestoreDto>
    suspend fun getParserConfigsVersion(): Long?
}
