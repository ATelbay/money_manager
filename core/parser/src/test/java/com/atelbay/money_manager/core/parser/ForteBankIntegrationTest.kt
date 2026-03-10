package com.atelbay.money_manager.core.parser

import com.atelbay.money_manager.core.model.TransactionType
import org.junit.Assert.*
import org.junit.Test

class ForteBankIntegrationTest {

    private val parser = RegexStatementParser()
    private val bankDetector = BankDetector()
    private val configs = ParserConfigTestFactory.getAllConfigs()

    @Test
    fun `forte bank is detected from real PDF`() {
        val text = PdfTestHelper.extractText("forte_statement.pdf")
        val config = bankDetector.detect(text, configs)
        assertNotNull("Forte bank must be detected to avoid AI fallback", config)
        assertEquals("forte", config!!.bankId)
    }

    @Test
    fun `forte real PDF parses correct transaction count`() {
        val text = PdfTestHelper.extractText("forte_statement.pdf")
        val config = bankDetector.detect(text, configs)!!
        val transactions = parser.parse(text, config)
        assertEquals("Expected exactly 80 transactions", 80, transactions.size)
        println("Forte: parsed ${transactions.size} transactions")
        transactions.take(5).forEach { println("  date=${it.date} amount=${it.amount} type=${it.type} details=${it.details}") }
    }

    @Test
    fun `forte negative sign means expense`() {
        val text = PdfTestHelper.extractText("forte_statement.pdf")
        val config = bankDetector.detect(text, configs)!!
        val transactions = parser.parse(text, config)
        assertTrue(transactions.isNotEmpty())
        // At least one EXPENSE should exist (negativeSignMeansExpense=true)
        val hasExpense = transactions.any { it.type == TransactionType.EXPENSE }
        assertTrue("Forte statement must have at least one EXPENSE transaction", hasExpense)
    }

    @Test
    fun `forte income transaction for account replenishment`() {
        val text = PdfTestHelper.extractText("forte_statement.pdf")
        val config = bankDetector.detect(text, configs)!!
        val transactions = parser.parse(text, config)
        // Look for "Пополнение счета" → INCOME
        val incomeTransactions = transactions.filter { it.type == TransactionType.INCOME }
        println("Forte INCOME transactions: ${incomeTransactions.size}")
        incomeTransactions.take(3).forEach { println("  ${it.operationType}: ${it.amount}") }
        // Assert there's at least one income (replenishment)
        assertTrue("Forte statement must have at least one INCOME transaction", incomeTransactions.isNotEmpty())
    }

    @Test
    fun `forte first transaction has correct values`() {
        val text = PdfTestHelper.extractText("forte_statement.pdf")
        val config = bankDetector.detect(text, configs)!!
        val transactions = parser.parse(text, config)
        assertTrue("Need at least 1 transaction", transactions.isNotEmpty())
        val first = transactions.first()
        assertEquals(TransactionType.EXPENSE, first.type)
        assertEquals(55000.0, first.amount, 0.01)
        assertEquals(kotlinx.datetime.LocalDate(2026, 3, 4), first.date)
        assertEquals("Перевод", first.operationType)
        println("Forte first: date=${first.date} amount=${first.amount} type=${first.type} op=${first.operationType} details=${first.details}")
    }
}
