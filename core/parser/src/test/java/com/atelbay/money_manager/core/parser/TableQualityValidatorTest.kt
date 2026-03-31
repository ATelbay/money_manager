package com.atelbay.money_manager.core.parser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TableQualityValidatorTest {

    private val validator = TableQualityValidator()

    // ─── Clean tables pass ───────────────────────────────────────────────────

    @Test
    fun `clean table with proper columns passes validation`() {
        val table = listOf(
            listOf("Дата", "Сумма", "Валюта", "Операция", "Детали"),
            listOf("01.03.2024", "5000.00", "KZT", "Покупка", "Магазин Электроника"),
            listOf("02.03.2024", "1200.50", "KZT", "Покупка", "Супермаркет Рамстор"),
            listOf("03.03.2024", "750.00", "KZT", "Перевод", "Перевод на карту Жания"),
            listOf("04.03.2024", "3000.00", "KZT", "Пополнение", "Зачисление зарплаты"),
        )
        val result = validator.validate(table)
        assertTrue(result.isAcceptable)
    }

    @Test
    fun `empty table passes validation`() {
        val result = validator.validate(emptyList())
        assertTrue(result.isAcceptable)
    }

    @Test
    fun `table with only header passes validation`() {
        val table = listOf(
            listOf("Дата", "Сумма", "Детали"),
        )
        val result = validator.validate(table)
        assertTrue(result.isAcceptable)
    }

    @Test
    fun `table with fewer than MIN_ROWS data rows passes validation`() {
        val table = listOf(
            listOf("Дата", "Сумма", "Детали"),
            listOf("01.03.2024", "5000.00", "Покупка"),
            listOf("02.03.2024", "1200.50", "Перевод"),
        )
        val result = validator.validate(table)
        assertTrue(result.isAcceptable)
    }

    // ─── Fragment detection ──────────────────────────────────────────────────

    @Test
    fun `table with lowercase-starting fragments is rejected`() {
        // Simulates Freedom Bank garbled extraction: "KZT обрабо Сумма" → fragments
        val table = listOf(
            listOf("Дата", "Сумма", "Валюта", "Операция", "Детали"),
            listOf("01.03.2024", "обрабо", "тке", "покупка", "в магазине"),
            listOf("02.03.2024", "полнение", "счета", "перевод", "на карту"),
            listOf("03.03.2024", "клада", "ция", "покупка", "аптека"),
            listOf("04.03.2024", "вление", "ных", "перевод", "зарплата"),
        )
        val result = validator.validate(table)
        assertFalse(result.isAcceptable)
        assertTrue(result.reason!!.contains("Fragment"))
    }

    @Test
    fun `table with mostly uppercase-starting cells passes fragment check`() {
        val table = listOf(
            listOf("Дата", "Сумма", "Операция", "Детали"),
            listOf("01.03.2024", "5000", "Покупка", "Магазин"),
            listOf("02.03.2024", "1200", "Перевод", "Карта"),
            listOf("03.03.2024", "750", "Покупка", "Аптека"),
            listOf("04.03.2024", "3000", "Пополнение", "Зарплата"),
        )
        val result = validator.validate(table)
        assertTrue(result.isAcceptable)
    }

    // ─── Short cell prevalence ───────────────────────────────────────────────

    @Test
    fun `table with many short cells is rejected`() {
        // Simulates Forte Bank over-split: "По" / "нк" from column boundaries splitting words
        val table = listOf(
            listOf("Дата", "A", "B", "C", "D", "E"),
            listOf("01.03.2024", "По", "нк", "50", "в", "KZ"),
            listOf("02.03.2024", "Пе", "ре", "12", "с", "KZ"),
            listOf("03.03.2024", "По", "ку", "75", "м", "KZ"),
            listOf("04.03.2024", "За", "рп", "30", "з", "KZ"),
        )
        val result = validator.validate(table)
        assertFalse(result.isAcceptable)
        assertTrue(result.reason!!.contains("Short cell") || result.reason!!.contains("Fragment"))
    }

    // ─── Date parse rate ─────────────────────────────────────────────────────

    @Test
    fun `table with shifted columns - no dates in first column - is rejected`() {
        // Column boundaries shifted: first column has non-date content
        val table = listOf(
            listOf("Shifted", "Data", "Here"),
            listOf("KZT обрабо", "5000.00", "Покупка"),
            listOf("KZT перев", "1200.50", "Перевод"),
            listOf("KZT попол", "750.00", "Пополнение"),
            listOf("KZT вывод", "3000.00", "Списание"),
        )
        val result = validator.validate(table)
        assertFalse(result.isAcceptable)
        assertTrue(result.reason!!.contains("Date parse rate"))
    }

    @Test
    fun `table with valid dates in first column passes date check`() {
        val table = listOf(
            listOf("Дата", "Сумма", "Детали"),
            listOf("01.03.2024", "5000.00", "Покупка"),
            listOf("02.03.2024", "1200.50", "Перевод"),
            listOf("03.03.2024", "750.00", "Пополнение"),
        )
        val result = validator.validate(table)
        assertTrue(result.isAcceptable)
    }

    @Test
    fun `table with mixed date formats passes date check`() {
        val table = listOf(
            listOf("Дата", "Сумма", "Детали"),
            listOf("2024-03-01", "5000.00", "Покупка"),
            listOf("2024-03-02", "1200.50", "Перевод"),
            listOf("01/03/2024", "750.00", "Пополнение"),
        )
        val result = validator.validate(table)
        assertTrue(result.isAcceptable)
    }

    // ─── Combined failures ───────────────────────────────────────────────────

    @Test
    fun `fragment check runs before short cell check`() {
        // Both checks would fail, but fragment runs first
        val table = listOf(
            listOf("X", "Y", "Z"),
            listOf("01.03.2024", "по", "нк"),
            listOf("02.03.2024", "пе", "ре"),
            listOf("03.03.2024", "по", "ку"),
        )
        val result = validator.validate(table)
        assertFalse(result.isAcceptable)
        assertTrue(result.reason!!.contains("Fragment"))
    }
}
