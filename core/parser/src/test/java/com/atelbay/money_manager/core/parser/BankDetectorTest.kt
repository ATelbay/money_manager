package com.atelbay.money_manager.core.parser

import com.atelbay.money_manager.core.remoteconfig.ParserConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class BankDetectorTest {

    private lateinit var detector: BankDetector
    private lateinit var kaspiConfig: ParserConfig
    private lateinit var halykConfig: ParserConfig

    @Before
    fun setUp() {
        detector = BankDetector()
        kaspiConfig = ParserConfig(
            bankId = "kaspi",
            bankMarkers = listOf("Kaspi Gold", "АО «Kaspi Bank»", "CASPKZKA"),
            transactionPattern = "",
            dateFormat = "dd.MM.yy",
            operationTypeMap = emptyMap(),
        )
        halykConfig = ParserConfig(
            bankId = "halyk",
            bankMarkers = listOf("Halyk Bank", "АО «Народный Банк Казахстана»"),
            transactionPattern = "",
            dateFormat = "dd.MM.yyyy",
            operationTypeMap = emptyMap(),
        )
    }

    @Test
    fun `detect Kaspi by marker in text`() {
        val text = "ВЫПИСКА\nпо Kaspi Gold за период с 14.01.26 по 14.02.26"

        val result = detector.detect(text, listOf(kaspiConfig, halykConfig))

        assertEquals("kaspi", result?.bankId)
    }

    @Test
    fun `detect Kaspi by footer marker`() {
        val text = "Транзакции...\nАО «Kaspi Bank», БИК CASPKZKA"

        val result = detector.detect(text, listOf(kaspiConfig))

        assertEquals("kaspi", result?.bankId)
    }

    @Test
    fun `return null for unknown bank`() {
        val text = "ВЫПИСКА\nпо карте Forte Bank за период"

        val result = detector.detect(text, listOf(kaspiConfig, halykConfig))

        assertNull(result)
    }

    @Test
    fun `return null for empty text`() {
        val result = detector.detect("", listOf(kaspiConfig))

        assertNull(result)
    }

    @Test
    fun `detect is case insensitive`() {
        val text = "выписка по kaspi gold"

        val result = detector.detect(text, listOf(kaspiConfig))

        assertEquals("kaspi", result?.bankId)
    }

    @Test
    fun `return first matching config when multiple match`() {
        val text = "Kaspi Gold - Halyk Bank"

        val result = detector.detect(text, listOf(halykConfig, kaspiConfig))

        assertEquals("halyk", result?.bankId)
    }

    @Test
    fun `return null for empty configs list`() {
        val text = "Kaspi Gold"

        val result = detector.detect(text, emptyList())

        assertNull(result)
    }
}
