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

interface FirestoreDataSource {

    suspend fun pushTransaction(userId: String, dto: TransactionDto)
    suspend fun pushAccount(userId: String, dto: AccountDto)
    suspend fun pushCategory(userId: String, dto: CategoryDto)
    suspend fun pushBudget(userId: String, dto: BudgetDto)
    suspend fun pushRecurringTransaction(userId: String, dto: RecurringTransactionDto)

    suspend fun pullTransactions(userId: String): List<TransactionDto>
    suspend fun pullAccounts(userId: String): List<AccountDto>
    suspend fun pullCategories(userId: String): List<CategoryDto>
    suspend fun pullBudgets(userId: String): List<BudgetDto>
    suspend fun pullRecurringTransactions(userId: String): List<RecurringTransactionDto>

    suspend fun findParserCandidate(bankId: String, transactionPattern: String): ParserCandidateDto?
    suspend fun findTableParserCandidate(bankId: String): ParserCandidateDto?
    suspend fun pushParserCandidate(dto: ParserCandidateDto)
    suspend fun incrementCandidateSuccessCount(candidateId: String)
    suspend fun findCandidatesByUser(userIdHash: String, configType: String): List<ParserCandidateDto>

    suspend fun pushDebt(userId: String, dto: DebtDto)
    suspend fun pullDebts(userId: String): List<DebtDto>
    suspend fun deleteDebt(userId: String, remoteId: String)

    suspend fun pushDebtPayment(userId: String, dto: DebtPaymentDto)
    suspend fun pullDebtPayments(userId: String): List<DebtPaymentDto>
    suspend fun deleteDebtPayment(userId: String, remoteId: String)

    suspend fun pullActiveParserConfigs(): List<RegexParserProfileFirestoreDto>
    suspend fun getParserConfigsVersion(): Long?
}
