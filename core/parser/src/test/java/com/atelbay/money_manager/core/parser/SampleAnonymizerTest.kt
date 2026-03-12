package com.atelbay.money_manager.core.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SampleAnonymizerTest {

    private lateinit var anonymizer: SampleAnonymizer

    @Before
    fun setUp() {
        anonymizer = SampleAnonymizer()
    }

    @Test
    fun `merchant names replaced with sequential placeholders`() {
        val input = """13.02.26   - 500,00 ₸   Покупка   TOO "KASPI MAGAZIN""""
        val result = anonymizer.anonymize(input)

        assertTrue(
            "Merchant should be replaced with MERCHANT_1, got: $result",
            result.contains("MERCHANT_1"),
        )
        assertTrue(
            "Original merchant name should not appear, got: $result",
            !result.contains("KASPI MAGAZIN"),
        )
    }

    @Test
    fun `same merchant gets same placeholder across lines`() {
        val input = """
            |13.02.26   - 500,00 ₸   Покупка   TOO "KASPI MAGAZIN"
            |14.02.26   - 300,00 ₸   Покупка   TOO "KASPI MAGAZIN"
        """.trimMargin()
        val result = anonymizer.anonymize(input)

        val lines = result.lines()
        // Both lines should have the same MERCHANT placeholder
        val merchantPattern = Regex("""MERCHANT_(\d+)""")
        val firstMatch = merchantPattern.find(lines[0])
        val secondMatch = merchantPattern.find(lines[1])

        assertEquals(
            "Same merchant should get same placeholder",
            firstMatch?.value,
            secondMatch?.value,
        )
    }

    @Test
    fun `different merchants get different placeholders`() {
        val input = """
            |13.02.26   - 500,00 ₸   Покупка   TOO "KASPI MAGAZIN"
            |13.02.26   - 4 020,00 ₸  Покупка   ТОО ОЗАР ФАРМ
        """.trimMargin()
        val result = anonymizer.anonymize(input)

        assertTrue("Should contain MERCHANT_1, got: $result", result.contains("MERCHANT_1"))
        assertTrue("Should contain MERCHANT_2, got: $result", result.contains("MERCHANT_2"))
    }

    @Test
    fun `card mask patterns removed`() {
        val input = "****1234 13.02.26 - 500,00 KZT Покупка"
        val result = anonymizer.anonymize(input)

        assertTrue(
            "Card mask should be removed, got: $result",
            !result.contains("****1234"),
        )
    }

    @Test
    fun `IBAN patterns removed`() {
        val input = "KZ123456789012345 13.02.26 - 500,00 KZT Покупка"
        val result = anonymizer.anonymize(input)

        assertTrue(
            "IBAN should be removed, got: $result",
            !result.contains("KZ123456789012345"),
        )
    }

    @Test
    fun `amounts preserved`() {
        val input = "13.02.26   - 10 000,50 ₸   Покупка   Merchant"
        val result = anonymizer.anonymize(input)

        assertTrue(
            "Amount 10 000,50 should be preserved, got: $result",
            result.contains("10 000,50"),
        )
    }

    @Test
    fun `amounts with dot decimal preserved`() {
        val input = "13.02.26   - 4 020,00 ₸   Покупка   Merchant"
        val result = anonymizer.anonymize(input)

        assertTrue(
            "Amount 4 020,00 should be preserved, got: $result",
            result.contains("4 020,00"),
        )
    }

    @Test
    fun `dates preserved`() {
        val input = "01.01.2026   - 500,00 ₸   Покупка   Merchant"
        val result = anonymizer.anonymize(input)

        assertTrue(
            "Date 01.01.2026 should be preserved, got: $result",
            result.contains("01.01.2026"),
        )
    }

    @Test
    fun `dates with slash preserved`() {
        val input = "13/02/2026   - 500,00 ₸   Покупка   Merchant"
        val result = anonymizer.anonymize(input)

        assertTrue(
            "Date 13/02/2026 should be preserved, got: $result",
            result.contains("13/02/2026"),
        )
    }

    @Test
    fun `currency codes preserved`() {
        val input = "13.02.26   - 500,00 KZT   Покупка   Merchant"
        val result = anonymizer.anonymize(input)

        assertTrue(
            "Currency code KZT should be preserved, got: $result",
            result.contains("KZT"),
        )
    }

    @Test
    fun `USD currency code preserved`() {
        val input = "13.02.26   - 100,00 USD   Покупка   Merchant"
        val result = anonymizer.anonymize(input)

        assertTrue(
            "Currency code USD should be preserved, got: $result",
            result.contains("USD"),
        )
    }

    @Test
    fun `empty input returns empty`() {
        assertEquals("", anonymizer.anonymize(""))
    }

    @Test
    fun `blank input returns blank`() {
        assertEquals("   ", anonymizer.anonymize("   "))
    }

    @Test
    fun `multi-line input with mixed content`() {
        val input = """
            |13.02.26   - 500,00 ₸   Покупка   TOO "KASPI MAGAZIN"
            |13.02.26   - 4 020,00 ₸  Покупка   ТОО ОЗАР ФАРМ
            |03.02.26   + 7 300,00 ₸  Пополнение  Рымжан Б.
            |01.02.26   - 150 000,00 ₸  Перевод   Жания Б.
        """.trimMargin()
        val result = anonymizer.anonymize(input)

        // Dates preserved
        assertTrue("Date 13.02.26 should be preserved", result.contains("13.02.26"))
        assertTrue("Date 03.02.26 should be preserved", result.contains("03.02.26"))
        assertTrue("Date 01.02.26 should be preserved", result.contains("01.02.26"))

        // Amounts preserved
        assertTrue("Amount 500,00 should be preserved", result.contains("500,00"))
        assertTrue("Amount 4 020,00 should be preserved", result.contains("4 020,00"))
        assertTrue("Amount 7 300,00 should be preserved", result.contains("7 300,00"))
        assertTrue("Amount 150 000,00 should be preserved", result.contains("150 000,00"))

        // Currency symbols preserved
        assertTrue("Currency symbol ₸ should be preserved", result.contains("₸"))

        // Operation words preserved
        assertTrue("Покупка should be preserved", result.contains("Покупка"))
        assertTrue("Пополнение should be preserved", result.contains("Пополнение"))
        assertTrue("Перевод should be preserved", result.contains("Перевод"))

        // Original merchant names removed
        assertTrue(
            "KASPI MAGAZIN should not appear",
            !result.contains("KASPI MAGAZIN"),
        )
        assertTrue(
            "ОЗАР ФАРМ should not appear",
            !result.contains("ОЗАР ФАРМ"),
        )
        assertTrue(
            "Рымжан should not appear",
            !result.contains("Рымжан"),
        )
        assertTrue(
            "Жания should not appear",
            !result.contains("Жания"),
        )

        // Should have multiple merchant placeholders
        assertTrue("Should contain MERCHANT_1", result.contains("MERCHANT_1"))
        assertTrue("Should contain MERCHANT_2", result.contains("MERCHANT_2"))
        assertTrue("Should contain MERCHANT_3", result.contains("MERCHANT_3"))
        assertTrue("Should contain MERCHANT_4", result.contains("MERCHANT_4"))
    }

    @Test
    fun `operation words are not treated as merchants`() {
        val input = "13.02.26   - 500,00 ₸   Покупка   SomeMerchant"
        val result = anonymizer.anonymize(input)

        assertTrue(
            "Покупка should be preserved as operation word, got: $result",
            result.contains("Покупка"),
        )
        assertTrue(
            "Merchant should be replaced, got: $result",
            !result.contains("SomeMerchant"),
        )
    }
}
