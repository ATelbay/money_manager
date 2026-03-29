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
        assertEquals(101, transactions.size)
    }

    @Test
    fun `freedom first transaction has correct values`() {
        val text = PdfTestHelper.extractText("freedom_statement.pdf")
        val config = bankDetector.detect(text, configs)!!
        val transactions = parser.parse(text, config)
        assertTrue("Need at least 1 transaction", transactions.isNotEmpty())
        val first = transactions.first()
        assertEquals(LocalDate(2026, 2, 25), first.date)
        assertEquals(9201.44, first.amount, 0.01)
        assertEquals(TransactionType.EXPENSE, first.type)
        assertEquals("Сумма в обработке", first.operationType)
        assertEquals("WOLT.COM ALMATY KZ", first.details)
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
