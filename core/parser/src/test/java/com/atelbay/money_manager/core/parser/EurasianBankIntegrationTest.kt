package com.atelbay.money_manager.core.parser

import com.atelbay.money_manager.core.model.TransactionType
import kotlinx.datetime.LocalDate
import org.junit.Assert.*
import org.junit.Test

class EurasianBankIntegrationTest {

    private val parser = RegexStatementParser()
    private val bankDetector = BankDetector()
    private val configs = ParserConfigTestFactory.getAllConfigs()

    @Test
    fun `eurasian bank is detected from real PDF`() {
        val text = PdfTestHelper.extractText("eurasian_statement.pdf")
        val config = bankDetector.detect(text, configs)
        assertNotNull("Eurasian bank must be detected to avoid AI fallback", config)
        assertEquals("eurasian", config!!.bankId)
    }

    @Test
    fun `eurasian real PDF parses transactions with deduplication`() {
        val text = PdfTestHelper.extractText("eurasian_statement.pdf")
        val config = bankDetector.detect(text, configs)!!

        // config has deduplicateMaxAmount=true — triplet rows for foreign-currency tx are collapsed
        val deduplicatedTransactions = parser.parse(text, config)
        assertEquals(31, deduplicatedTransactions.size)
        println("Eurasian: ${deduplicatedTransactions.size} transactions after dedup")
        deduplicatedTransactions.take(5).forEach {
            println("  date=${it.date} amount=${it.amount} type=${it.type} details=${it.details}")
        }
    }

    @Test
    fun `eurasian first transaction has correct values`() {
        val text = PdfTestHelper.extractText("eurasian_statement.pdf")
        val config = bankDetector.detect(text, configs)!!
        val transactions = parser.parse(text, config)
        assertTrue("Need at least 1 transaction", transactions.isNotEmpty())
        val first = transactions.first()
        assertEquals(LocalDate(2026, 1, 12), first.date)
        assertEquals(66819.63, first.amount, 0.01)
        assertEquals(TransactionType.EXPENSE, first.type)
        println("Eurasian first: date=${first.date} amount=${first.amount} type=${first.type} details=${first.details}")
    }

    @Test
    fun `eurasian deduplication reduces triplet rows`() {
        // Eurasian emits up to 3 rows per foreign-currency transaction
        // deduplicateMaxAmount=true keeps only the max-amount row per (date, details) group
        val text = PdfTestHelper.extractText("eurasian_statement.pdf")
        val configWithDedup = bankDetector.detect(text, configs)!! // has deduplicateMaxAmount=true
        val configWithoutDedup = configWithDedup.copy(deduplicateMaxAmount = false)

        val deduped = parser.parse(text, configWithDedup)
        val raw = parser.parse(text, configWithoutDedup)

        println("Eurasian raw: ${raw.size}, after dedup: ${deduped.size}")
        // PDF has foreign-currency transactions: raw count (69) > deduped count (31)
        assertEquals(31, deduped.size)
        assertEquals(69, raw.size)
        assertTrue("Dedup should not increase transaction count", deduped.size <= raw.size)
    }
}
