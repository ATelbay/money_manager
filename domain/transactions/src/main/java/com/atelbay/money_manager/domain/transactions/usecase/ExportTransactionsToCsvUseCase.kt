package com.atelbay.money_manager.domain.transactions.usecase

import com.atelbay.money_manager.domain.accounts.repository.AccountRepository
import com.atelbay.money_manager.domain.transactions.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class ExportTransactionsToCsvUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
) {
    suspend operator fun invoke(): String {
        val transactions = transactionRepository.observeAll().first()
        val accounts = accountRepository.observeAll().first()
        val accountMap = accounts.associate { it.id to it.name }

        val dateFormatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault())

        val sb = StringBuilder()
        sb.appendLine("Date,Type,Amount,Category,Account,Note")

        for (tx in transactions) {
            val date = dateFormatter.format(Instant.ofEpochMilli(tx.date))
            val type = tx.type.name.lowercase()
            val amount = java.math.BigDecimal(tx.amount).stripTrailingZeros().toPlainString()
            val category = csvQuote(tx.categoryName)
            val account = csvQuote(accountMap[tx.accountId] ?: "")
            val note = csvQuote(tx.note ?: "")
            sb.appendLine("$date,$type,$amount,$category,$account,$note")
        }

        return sb.toString()
    }

    private fun csvQuote(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
