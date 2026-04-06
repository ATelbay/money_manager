package com.atelbay.money_manager.core.parser

import com.atelbay.money_manager.core.model.TransactionType
import kotlinx.datetime.LocalDate
import org.junit.Assert.*
import org.junit.Test

class KaspiBankIntegrationTest {

    private val parser = RegexStatementParser()
    private val bankDetector = BankDetector()
    private val configs = RegexParserProfileTestFactory.getAllConfigs()

    @Test
    fun `kaspi bank is detected from real PDF`() {
        val text = PdfTestHelper.extractText("gold_statement.pdf")
        val config = bankDetector.detect(text, configs)
        assertNotNull("Kaspi bank must be detected to avoid AI fallback", config)
        assertEquals("kaspi", config!!.bankId)
    }

    @Test
    fun `kaspi real PDF parses correct transaction count`() {
        val text = PdfTestHelper.extractText("gold_statement.pdf")
        val config = bankDetector.detect(text, configs)!!
        val transactions = parser.parse(text, config)
        assertEquals(34, transactions.size)
    }

    @Test
    fun `kaspi first transaction has correct values`() {
        val text = PdfTestHelper.extractText("gold_statement.pdf")
        val config = bankDetector.detect(text, configs)!!
        val transactions = parser.parse(text, config)
        assertTrue("Need at least 1 transaction", transactions.isNotEmpty())
        val first = transactions.first()
        assertEquals(LocalDate(2026, 2, 13), first.date)
        assertEquals(500.0, first.amount, 0.01)
        assertEquals(TransactionType.EXPENSE, first.type)
        assertEquals("TOO \"KASPI MAGAZIN\"", first.details)
    }
}
