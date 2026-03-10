package com.atelbay.money_manager.core.parser

import com.atelbay.money_manager.core.model.TransactionType
import kotlinx.datetime.LocalDate
import org.junit.Assert.*
import org.junit.Test

class FreedomBankIntegrationTest {

    private val parser = RegexStatementParser()
    private val bankDetector = BankDetector()
    private val configs = ParserConfigTestFactory.getAllConfigs()

    @Test
    fun `freedom bank is detected from real PDF`() {
        val text = PdfTestHelper.extractText("freedom_statement.pdf")
        val config = bankDetector.detect(text, configs)
        assertNotNull("Freedom bank must be detected to avoid AI fallback", config)
        assertEquals("freedom", config!!.bankId)
    }

    @Test
    fun `freedom real PDF parses correct transaction count`() {
        val text = PdfTestHelper.extractText("freedom_statement.pdf")
        val config = bankDetector.detect(text, configs)!!
        val transactions = parser.parse(text, config)
        assertEquals(93, transactions.size)
    }

    @Test
    fun `freedom first transaction has correct values`() {
        val text = PdfTestHelper.extractText("freedom_statement.pdf")
        val config = bankDetector.detect(text, configs)!!
        val transactions = parser.parse(text, config)
        assertTrue("Need at least 1 transaction", transactions.isNotEmpty())
        val first = transactions.first()
        assertEquals(LocalDate(2026, 2, 24), first.date)
        assertEquals(28774.53, first.amount, 0.01)
        assertEquals(TransactionType.INCOME, first.type)
        assertEquals("KZ12551B529955307KZT. По  договору №SRV-0075537 от", first.details)
    }

    @Test
    fun `freedom multi-line descriptions are joined correctly`() {
        val text = PdfTestHelper.extractText("freedom_statement.pdf")
        val config = bankDetector.detect(text, configs)!!
        val transactions = parser.parse(text, config)
        // Details should not be truncated mid-word (no trailing spaces suggesting cut-off)
        assertTrue(transactions.isNotEmpty())
        transactions.forEach { tx ->
            assertFalse("Details should not end with space: '${tx.details}'", tx.details.endsWith(" "))
        }
    }
}
