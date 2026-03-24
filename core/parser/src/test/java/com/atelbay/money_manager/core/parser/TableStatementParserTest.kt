package com.atelbay.money_manager.core.parser

import com.atelbay.money_manager.core.model.TableParserConfig
import com.atelbay.money_manager.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TableStatementParserTest {

    private val parser = TableStatementParser()

    // ==================== HELPER ====================

    /**
     * Builds a [TableParserConfig] with sensible defaults for testing.
     * Callers override only the fields relevant to the scenario under test.
     */
    private fun buildConfig(
        bankId: String = "test_bank",
        bankMarkers: List<String> = listOf("Test Bank"),
        dateColumn: Int = 0,
        amountColumn: Int = 1,
        operationColumn: Int? = null,
        detailsColumn: Int? = null,
        signColumn: Int? = null,
        dateFormat: String = "dd.MM.yyyy",
        amountFormat: String = "dot",
        negativeSignMeansExpense: Boolean = true,
        skipHeaderRows: Int = 1,
        deduplicateMaxAmount: Boolean = false,
        operationTypeMap: Map<String, String> = emptyMap(),
    ) = TableParserConfig(
        bankId = bankId,
        bankMarkers = bankMarkers,
        dateColumn = dateColumn,
        amountColumn = amountColumn,
        operationColumn = operationColumn,
        detailsColumn = detailsColumn,
        signColumn = signColumn,
        dateFormat = dateFormat,
        amountFormat = amountFormat,
        negativeSignMeansExpense = negativeSignMeansExpense,
        skipHeaderRows = skipHeaderRows,
        deduplicateMaxAmount = deduplicateMaxAmount,
        operationTypeMap = operationTypeMap,
    )

    // ==================== 1. 4-COLUMN TABLE (FORTE-LIKE) ====================

    @Test
    fun `4-column Forte-like table parses date amount operation details correctly`() {
        // Columns: date | amount | operation | details
        val table = listOf(
            listOf("Дата", "Сумма", "Операция", "Детали"), // header
            listOf("15.03.2024", "-5000.00", "Покупка", "Магазин Апорт"),
            listOf("16.03.2024", "10000.00", "Пополнение", "Перевод от Иванова"),
            listOf("17.03.2024", "-2500.00", "Перевод", "Оплата услуг"),
        )
        val config = buildConfig(
            dateColumn = 0,
            amountColumn = 1,
            operationColumn = 2,
            detailsColumn = 3,
            amountFormat = "dot",
            negativeSignMeansExpense = true,
            skipHeaderRows = 1,
        )

        val result = parser.parse(table, config)

        assertEquals(3, result.size)

        assertEquals(5000.0, result[0].amount, 0.01)
        assertEquals(TransactionType.EXPENSE, result[0].type)
        assertEquals("Покупка", result[0].operationType)
        assertEquals("Магазин Апорт", result[0].details)

        assertEquals(10000.0, result[1].amount, 0.01)
        assertEquals(TransactionType.INCOME, result[1].type)
        assertEquals("Пополнение", result[1].operationType)
        assertEquals("Перевод от Иванова", result[1].details)

        assertEquals(2500.0, result[2].amount, 0.01)
        assertEquals(TransactionType.EXPENSE, result[2].type)
        assertEquals("Перевод", result[2].operationType)
        assertEquals("Оплата услуг", result[2].details)
    }

    @Test
    fun `4-column table date parsed into correct LocalDate fields`() {
        val table = listOf(
            listOf("Дата", "Сумма", "Тип", "Описание"),
            listOf("01.07.2025", "999.00", "Покупка", "Кофе"),
        )
        val config = buildConfig(
            dateColumn = 0,
            amountColumn = 1,
            operationColumn = 2,
            detailsColumn = 3,
            amountFormat = "dot",
            negativeSignMeansExpense = false,
            skipHeaderRows = 1,
        )

        val result = parser.parse(table, config)

        assertEquals(1, result.size)
        assertEquals(2025, result[0].date.year)
        assertEquals(7, result[0].date.monthNumber)
        assertEquals(1, result[0].date.dayOfMonth)
    }

    // ==================== 2. 7-COLUMN TABLE (BEREKE-LIKE) ====================

    @Test
    fun `7-column Bereke-like table with sign column and comma_dot format`() {
        // Columns: date | txId | sign | amount | currency | operation | details
        val table = listOf(
            listOf("Date", "TxID", "Sign", "Amount", "Currency", "Type", "Description"), // header
            listOf("15.03.2024", "TX001", "-", "1,234.56", "KZT", "Purchase", "Supermarket"),
            listOf("16.03.2024", "TX002", "+", "50,000.00", "KZT", "Transfer", "Salary"),
            listOf("17.03.2024", "TX003", "-", "300.00", "KZT", "Payment", "Utility"),
        )
        val config = buildConfig(
            dateColumn = 0,
            amountColumn = 3,
            operationColumn = 5,
            detailsColumn = 6,
            signColumn = 2,
            amountFormat = "comma_dot",
            negativeSignMeansExpense = false,
            skipHeaderRows = 1,
        )

        val result = parser.parse(table, config)

        assertEquals(3, result.size)

        assertEquals(1234.56, result[0].amount, 0.01)
        assertEquals(TransactionType.EXPENSE, result[0].type)
        assertEquals("Supermarket", result[0].details)

        assertEquals(50000.0, result[1].amount, 0.01)
        assertEquals(TransactionType.INCOME, result[1].type)
        assertEquals("Salary", result[1].details)

        assertEquals(300.0, result[2].amount, 0.01)
        assertEquals(TransactionType.EXPENSE, result[2].type)
        assertEquals("Utility", result[2].details)
    }

    // ==================== 3. AMOUNT FORMATS ====================

    @Test
    fun `dot format parses 1234_56 correctly`() {
        val table = listOf(
            listOf("15.03.2024", "1234.56", "Покупка", "Details"),
        )
        val config = buildConfig(
            amountFormat = "dot",
            negativeSignMeansExpense = false,
            skipHeaderRows = 0,
        )

        val result = parser.parse(table, config)

        assertEquals(1, result.size)
        assertEquals(1234.56, result[0].amount, 0.001)
    }

    @Test
    fun `comma_dot format parses 1_234_56 with comma thousands separator correctly`() {
        // "comma_dot": comma is thousands separator, dot is decimal → strip commas
        val table = listOf(
            listOf("15.03.2024", "1,234.56", "Purchase", "Details"),
        )
        val config = buildConfig(
            amountFormat = "comma_dot",
            negativeSignMeansExpense = false,
            skipHeaderRows = 0,
        )

        val result = parser.parse(table, config)

        assertEquals(1, result.size)
        assertEquals(1234.56, result[0].amount, 0.001)
    }

    @Test
    fun `space_comma format parses 1 234_56 with space separator correctly`() {
        val table = listOf(
            listOf("15.03.2024", "1 234,56", "Purchase", "Details"),
        )
        val config = buildConfig(
            amountFormat = "space_comma",
            negativeSignMeansExpense = false,
            skipHeaderRows = 0,
        )

        val result = parser.parse(table, config)

        assertEquals(1, result.size)
        assertEquals(1234.56, result[0].amount, 0.001)
    }

    @Test
    fun `dot format handles negative amount and returns absolute value`() {
        val table = listOf(
            listOf("15.03.2024", "-9876.54", "Expense", "Details"),
        )
        val config = buildConfig(
            amountFormat = "dot",
            negativeSignMeansExpense = true,
            skipHeaderRows = 0,
        )

        val result = parser.parse(table, config)

        assertEquals(1, result.size)
        assertEquals(9876.54, result[0].amount, 0.001)
        assertEquals(TransactionType.EXPENSE, result[0].type)
    }

    // ==================== 4. SIGN COLUMN DETECTION ====================

    @Test
    fun `sign column plus maps to INCOME`() {
        val table = listOf(
            listOf("15.03.2024", "500.00", "+", "Transfer"),
        )
        val config = buildConfig(
            amountColumn = 1,
            signColumn = 2,
            detailsColumn = 3,
            amountFormat = "dot",
            negativeSignMeansExpense = false,
            skipHeaderRows = 0,
        )

        val result = parser.parse(table, config)

        assertEquals(1, result.size)
        assertEquals(TransactionType.INCOME, result[0].type)
    }

    @Test
    fun `sign column minus maps to EXPENSE`() {
        val table = listOf(
            listOf("15.03.2024", "500.00", "-", "Purchase"),
        )
        val config = buildConfig(
            amountColumn = 1,
            signColumn = 2,
            detailsColumn = 3,
            amountFormat = "dot",
            negativeSignMeansExpense = false,
            skipHeaderRows = 0,
        )

        val result = parser.parse(table, config)

        assertEquals(1, result.size)
        assertEquals(TransactionType.EXPENSE, result[0].type)
    }

    @Test
    fun `sign column takes precedence when negativeSignMeansExpense is false`() {
        // Positive amount but sign column says "-" → EXPENSE
        val table = listOf(
            listOf("15.03.2024", "1000.00", "-", "Grocery"),
        )
        val config = buildConfig(
            amountColumn = 1,
            signColumn = 2,
            amountFormat = "dot",
            negativeSignMeansExpense = false,
            skipHeaderRows = 0,
        )

        val result = parser.parse(table, config)

        assertEquals(1, result.size)
        assertEquals(TransactionType.EXPENSE, result[0].type)
    }

    @Test
    fun `negativeSignMeansExpense true with negative amount gives EXPENSE`() {
        val table = listOf(
            listOf("20.05.2024", "-750.00", "Оплата", "Интернет"),
        )
        val config = buildConfig(
            operationColumn = 2,
            detailsColumn = 3,
            amountFormat = "dot",
            negativeSignMeansExpense = true,
            skipHeaderRows = 0,
        )

        val result = parser.parse(table, config)

        assertEquals(1, result.size)
        assertEquals(TransactionType.EXPENSE, result[0].type)
        assertEquals(750.0, result[0].amount, 0.001)
    }

    @Test
    fun `negativeSignMeansExpense true with positive amount gives INCOME`() {
        val table = listOf(
            listOf("20.05.2024", "5000.00", "Пополнение", "Зарплата"),
        )
        val config = buildConfig(
            operationColumn = 2,
            detailsColumn = 3,
            amountFormat = "dot",
            negativeSignMeansExpense = true,
            skipHeaderRows = 0,
        )

        val result = parser.parse(table, config)

        assertEquals(1, result.size)
        assertEquals(TransactionType.INCOME, result[0].type)
    }

    // ==================== 5. HEADER ROW SKIPPING ====================

    @Test
    fun `skipHeaderRows=0 processes all rows including first`() {
        val table = listOf(
            listOf("01.01.2025", "100.00", "Op", "Detail"),
            listOf("02.01.2025", "200.00", "Op", "Detail"),
        )
        val config = buildConfig(skipHeaderRows = 0)

        val result = parser.parse(table, config)

        assertEquals(2, result.size)
        assertEquals(100.0, result[0].amount, 0.01)
        assertEquals(200.0, result[1].amount, 0.01)
    }

    @Test
    fun `skipHeaderRows=1 skips only first row`() {
        val table = listOf(
            listOf("Date", "Amount", "Op", "Details"), // header — should be skipped
            listOf("01.01.2025", "100.00", "Op", "Detail"),
            listOf("02.01.2025", "200.00", "Op", "Detail"),
        )
        val config = buildConfig(skipHeaderRows = 1)

        val result = parser.parse(table, config)

        assertEquals(2, result.size)
        assertEquals(100.0, result[0].amount, 0.01)
        assertEquals(200.0, result[1].amount, 0.01)
    }

    @Test
    fun `skipHeaderRows=2 skips first two rows`() {
        val table = listOf(
            listOf("Bank Name", "Statement Period"), // header row 1
            listOf("Date", "Amount", "Op", "Details"), // header row 2 — both skipped
            listOf("03.01.2025", "300.00", "Op", "Detail"),
            listOf("04.01.2025", "400.00", "Op", "Detail"),
        )
        val config = buildConfig(skipHeaderRows = 2)

        val result = parser.parse(table, config)

        assertEquals(2, result.size)
        assertEquals(300.0, result[0].amount, 0.01)
        assertEquals(400.0, result[1].amount, 0.01)
    }

    // ==================== 6. DEDUPLICATE MAX AMOUNT ====================

    @Test
    fun `deduplicateMaxAmount keeps max amount per date-details group`() {
        // Eurasian-like: 3 rows per foreign-currency tx → keep max amount
        val table = listOf(
            listOf("10.02.2025", "5000.00", "KZT row", "Coffee"),
            listOf("10.02.2025", "15.00", "USD row", "Coffee"),
            listOf("10.02.2025", "7500.00", "Converted row", "Coffee"),
            listOf("11.02.2025", "200.00", "Op", "Grocery"),
        )
        val config = buildConfig(
            operationColumn = 2,
            detailsColumn = 3,
            amountFormat = "dot",
            negativeSignMeansExpense = false,
            skipHeaderRows = 0,
            deduplicateMaxAmount = true,
        )

        val result = parser.parse(table, config)

        // "Coffee" on 10.02.2025 → keep max = 7500.00; "Grocery" on 11.02.2025 → keep 200.00
        assertEquals(2, result.size)
        val coffeeRow = result.first { it.details == "Coffee" }
        val groceryRow = result.first { it.details == "Grocery" }
        assertEquals(7500.0, coffeeRow.amount, 0.01)
        assertEquals(200.0, groceryRow.amount, 0.01)
    }

    @Test
    fun `deduplicateMaxAmount false keeps all rows even when date-details match`() {
        val table = listOf(
            listOf("10.02.2025", "5000.00", "Op", "Coffee"),
            listOf("10.02.2025", "7500.00", "Op", "Coffee"),
        )
        val config = buildConfig(
            detailsColumn = 3,
            amountFormat = "dot",
            negativeSignMeansExpense = false,
            skipHeaderRows = 0,
            deduplicateMaxAmount = false,
        )

        val result = parser.parse(table, config)

        assertEquals(2, result.size)
    }

    // ==================== 7. OUT-OF-BOUNDS COLUMN INDEX ====================

    @Test
    fun `out-of-bounds dateColumn causes row to be skipped gracefully`() {
        val table = listOf(
            listOf("01.01.2025", "100.00"), // only 2 columns
        )
        // dateColumn=5 is out of bounds → row should be skipped, no exception thrown
        val config = buildConfig(
            dateColumn = 5,
            amountColumn = 1,
            skipHeaderRows = 0,
        )

        val result = parser.parse(table, config)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `out-of-bounds amountColumn causes row to be skipped gracefully`() {
        val table = listOf(
            listOf("01.01.2025", "100.00"), // only 2 columns
        )
        // amountColumn=10 is out of bounds → row should be skipped
        val config = buildConfig(
            dateColumn = 0,
            amountColumn = 10,
            skipHeaderRows = 0,
        )

        val result = parser.parse(table, config)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `out-of-bounds optional columns are silently ignored`() {
        val table = listOf(
            listOf("01.01.2025", "100.00"),
        )
        // detailsColumn=99 is out of bounds but optional → row parses with empty details
        val config = buildConfig(
            dateColumn = 0,
            amountColumn = 1,
            detailsColumn = 99,
            amountFormat = "dot",
            negativeSignMeansExpense = false,
            skipHeaderRows = 0,
        )

        val result = parser.parse(table, config)

        assertEquals(1, result.size)
        assertEquals(100.0, result[0].amount, 0.01)
        assertEquals("", result[0].details)
    }

    @Test
    fun `negative dateColumn throws IllegalArgumentException`() {
        val table = listOf(listOf("01.01.2025", "100.00"))
        val config = buildConfig(dateColumn = -1, amountColumn = 1, skipHeaderRows = 0)

        try {
            parser.parse(table, config)
            fail("Expected IllegalArgumentException for negative dateColumn")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("dateColumn"))
        }
    }

    @Test
    fun `negative amountColumn throws IllegalArgumentException`() {
        val table = listOf(listOf("01.01.2025", "100.00"))
        val config = buildConfig(dateColumn = 0, amountColumn = -2, skipHeaderRows = 0)

        try {
            parser.parse(table, config)
            fail("Expected IllegalArgumentException for negative amountColumn")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("amountColumn"))
        }
    }

    @Test
    fun `negative skipHeaderRows throws IllegalArgumentException`() {
        val table = listOf(listOf("01.01.2025", "100.00"))
        val config = buildConfig(dateColumn = 0, amountColumn = 1, skipHeaderRows = -1)

        try {
            parser.parse(table, config)
            fail("Expected IllegalArgumentException for negative skipHeaderRows")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("skipHeaderRows"))
        }
    }

    // ==================== 8. EMPTY TABLE ====================

    @Test
    fun `empty table returns empty result`() {
        val result = parser.parse(emptyList(), buildConfig())

        assertTrue(result.isEmpty())
    }

    @Test
    fun `table with only header rows returns empty result`() {
        val table = listOf(
            listOf("Date", "Amount", "Operation", "Details"),
        )
        val config = buildConfig(skipHeaderRows = 1)

        val result = parser.parse(table, config)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `table with blank date and amount cells skips those rows`() {
        val table = listOf(
            listOf("", "", "Op", "Detail"),
            listOf("   ", "   ", "Op", "Detail"),
            listOf("01.01.2025", "100.00", "Op", "Detail"),
        )
        val config = buildConfig(
            operationColumn = 2,
            detailsColumn = 3,
            amountFormat = "dot",
            negativeSignMeansExpense = false,
            skipHeaderRows = 0,
        )

        val result = parser.parse(table, config)

        assertEquals(1, result.size)
        assertEquals(100.0, result[0].amount, 0.01)
    }

    // ==================== 9. SPACE_DOT AMOUNT FORMAT ====================

    @Test
    fun `space_dot format parses 100 000 dot 00 correctly`() {
        val table = listOf(
            listOf("15.03.2024", "100 000.00", "Purchase", "Details"),
        )
        val config = buildConfig(
            amountFormat = "space_dot",
            negativeSignMeansExpense = false,
            skipHeaderRows = 0,
        )

        val result = parser.parse(table, config)

        assertEquals(1, result.size)
        assertEquals(100000.0, result[0].amount, 0.001)
    }

    @Test
    fun `space_dot format handles negative amount`() {
        val table = listOf(
            listOf("15.03.2024", "-10 000.00", "Purchase", "Details"),
        )
        val config = buildConfig(
            amountFormat = "space_dot",
            negativeSignMeansExpense = true,
            skipHeaderRows = 0,
        )

        val result = parser.parse(table, config)

        assertEquals(1, result.size)
        assertEquals(10000.0, result[0].amount, 0.001)
        assertEquals(TransactionType.EXPENSE, result[0].type)
    }

    @Test
    fun `space_dot format parses amount with currency suffix`() {
        val table = listOf(
            listOf("15.03.2024", "100 000.00 KZT", "Purchase", "Details"),
        )
        val config = buildConfig(
            amountFormat = "space_dot",
            negativeSignMeansExpense = false,
            skipHeaderRows = 0,
        )

        val result = parser.parse(table, config)

        assertEquals(1, result.size)
        assertEquals(100000.0, result[0].amount, 0.001)
    }

    // ==================== 10. EXTRACT FIRST DATE ====================

    @Test
    fun `double date cell extracts first date yyyy-MM-dd`() {
        val table = listOf(
            listOf("2024-12-31 2024-12-31", "500.00", "Op", "Detail"),
        )
        val config = buildConfig(
            dateFormat = "yyyy-MM-dd",
            amountFormat = "dot",
            negativeSignMeansExpense = false,
            skipHeaderRows = 0,
        )

        val result = parser.parse(table, config)

        assertEquals(1, result.size)
        assertEquals(2024, result[0].date.year)
        assertEquals(12, result[0].date.monthNumber)
        assertEquals(31, result[0].date.dayOfMonth)
    }

    @Test
    fun `date cell with extra text extracts date`() {
        val table = listOf(
            listOf("2024-12-31 KZT", "500.00", "Op", "Detail"),
        )
        val config = buildConfig(
            dateFormat = "yyyy-MM-dd",
            amountFormat = "dot",
            negativeSignMeansExpense = false,
            skipHeaderRows = 0,
        )

        val result = parser.parse(table, config)

        assertEquals(1, result.size)
        assertEquals(2024, result[0].date.year)
        assertEquals(12, result[0].date.monthNumber)
        assertEquals(31, result[0].date.dayOfMonth)
    }

    @Test
    fun `single clean date parses normally`() {
        val table = listOf(
            listOf("31.03.2024", "500.00", "Op", "Detail"),
        )
        val config = buildConfig(
            dateFormat = "dd.MM.yyyy",
            amountFormat = "dot",
            negativeSignMeansExpense = false,
            skipHeaderRows = 0,
        )

        val result = parser.parse(table, config)

        assertEquals(1, result.size)
        assertEquals(2024, result[0].date.year)
        assertEquals(3, result[0].date.monthNumber)
        assertEquals(31, result[0].date.dayOfMonth)
    }

    @Test
    fun `double date with time extracts only first date dd_MM_yyyy`() {
        // Cell contains date + time + repeated date — extractFirstDate should return "25.03.2026"
        val table = listOf(
            listOf("25.03.2026 14:30:00 25.03.2026", "1000.00", "Op", "Detail"),
        )
        val config = buildConfig(
            dateFormat = "dd.MM.yyyy",
            amountFormat = "dot",
            negativeSignMeansExpense = false,
            skipHeaderRows = 0,
        )

        val result = parser.parse(table, config)

        assertEquals(1, result.size)
        assertEquals(2026, result[0].date.year)
        assertEquals(3, result[0].date.monthNumber)
        assertEquals(25, result[0].date.dayOfMonth)
    }

    @Test
    fun `date cell with no matching date falls back to trim and row returns null`() {
        val table = listOf(
            listOf("NoDate", "500.00", "Op", "Detail"),
        )
        val config = buildConfig(
            dateFormat = "dd.MM.yyyy",
            amountFormat = "dot",
            negativeSignMeansExpense = false,
            skipHeaderRows = 0,
        )

        val result = parser.parse(table, config)

        assertTrue(result.isEmpty())
    }

    // ==================== 11. OPERATION TYPE MAP ====================

    @Test
    fun `operationTypeMap maps operation text to INCOME when no sign info`() {
        val table = listOf(
            listOf("Дата", "Сумма", "Операция"),
            listOf("15.01.2025", "5000.00", "Покупка"),
            listOf("16.01.2025", "10000.00", "Пополнение"),
        )
        val config = buildConfig(
            operationColumn = 2,
            negativeSignMeansExpense = false,
            operationTypeMap = mapOf("Покупка" to "expense", "Пополнение" to "income"),
        )

        val result = parser.parse(table, config)

        assertEquals(2, result.size)
        assertEquals(TransactionType.EXPENSE, result[0].type)
        assertEquals(TransactionType.INCOME, result[1].type)
    }

    @Test
    fun `operationTypeMap defaults to EXPENSE for unmapped operation`() {
        val table = listOf(
            listOf("Дата", "Сумма", "Операция"),
            listOf("15.01.2025", "5000.00", "Unknown Op"),
        )
        val config = buildConfig(
            operationColumn = 2,
            negativeSignMeansExpense = false,
            operationTypeMap = mapOf("Покупка" to "expense", "Пополнение" to "income"),
        )

        val result = parser.parse(table, config)

        assertEquals(1, result.size)
        assertEquals(TransactionType.EXPENSE, result[0].type)
    }
}
