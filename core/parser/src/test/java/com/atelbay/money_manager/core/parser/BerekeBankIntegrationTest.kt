package com.atelbay.money_manager.core.parser

import com.atelbay.money_manager.core.model.TransactionType
import kotlinx.datetime.LocalDate
import org.junit.Assert.*
import org.junit.Test

class BerekeBankIntegrationTest {

    private val parser = RegexStatementParser()
    private val bankDetector = BankDetector()
    private val configs = RegexParserProfileTestFactory.getAllConfigs()

    @Test
    fun `bereke bank is detected from real PDF`() {
        val text = PdfTestHelper.extractText("bereke_statement.pdf")
        val config = bankDetector.detect(text, configs)
        assertNotNull("Bank must be detected to avoid AI fallback", config)
        assertEquals("bereke", config!!.bankId)
    }

    @Test
    fun `bereke real PDF parses correct transaction count`() {
        val text = PdfTestHelper.extractText("bereke_statement.pdf")
        val config = bankDetector.detect(text, configs)!!
        val transactions = parser.parse(text, config)
        assertEquals("Expected exactly 163 transactions", 163, transactions.size)
        println("Bereke: parsed ${transactions.size} transactions")
        transactions.take(5).forEach { println("  $it") }
    }

    @Test
    fun `bereke first transaction has correct values`() {
        val text = PdfTestHelper.extractText("bereke_statement.pdf")
        val config = bankDetector.detect(text, configs)!!
        val transactions = parser.parse(text, config)
        assertTrue("Need at least 1 transaction", transactions.isNotEmpty())
        val first = transactions.first()
        assertEquals(LocalDate(2025, 3, 7), first.date)
        assertEquals(20000.0, first.amount, 0.01)
        assertEquals(TransactionType.EXPENSE, first.type)
        assertEquals("JSC Eurasian Bank", first.details)
        println("First transaction: date=${first.date}, amount=${first.amount}, type=${first.type}, details=${first.details}")
    }
}
