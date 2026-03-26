package com.atelbay.money_manager.core.parser

import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [PdfTableExtractor.mergeMultiLineRows] and [PdfTableExtractor.stripPageHeaders].
 *
 * These functions are `internal` with `@VisibleForTesting`, so they can be called directly here
 * without going through [PdfTableExtractor.extractTable] (which requires Android PdfBox runtime).
 */
class PdfTableExtractorMergeTest {

    private lateinit var extractor: PdfTableExtractor

    @Before
    fun setUp() {
        extractor = PdfTableExtractor(mockk(relaxed = true))
    }

    // ─── 1. Empty input ───────────────────────────────────────────────────────

    @Test
    fun `empty input returns empty output`() {
        val result = extractor.mergeMultiLineRows(emptyList())
        assertTrue(result.isEmpty())
    }

    // ─── 2. All date rows pass through unchanged ──────────────────────────────

    @Test
    fun `single-line rows starting with date pass through unchanged`() {
        val rows = listOf(
            listOf("01.03.2024", "Purchase", "1000", "50000"),
            listOf("02.03.2024", "Transfer", "2000", "48000"),
            listOf("03.03.2024", "Withdrawal", "500", "47500"),
        )
        val result = extractor.mergeMultiLineRows(rows)

        assertEquals(3, result.size)
        assertEquals(listOf("01.03.2024", "Purchase", "1000", "50000"), result[0])
        assertEquals(listOf("02.03.2024", "Transfer", "2000", "48000"), result[1])
        assertEquals(listOf("03.03.2024", "Withdrawal", "500", "47500"), result[2])
    }

    // ─── 3. Single continuation row merges into parent ────────────────────────

    @Test
    fun `continuation row merges into preceding date row`() {
        val rows = listOf(
            listOf("01.03.2024", "First line", "1000", "50000"),
            listOf("", "continued desc", "", ""),       // continuation: first cell has no date
        )
        val result = extractor.mergeMultiLineRows(rows)

        assertEquals(1, result.size)
        assertEquals("01.03.2024", result[0][0])
        assertEquals("First line continued desc", result[0][1])
        // Amount and balance cells: empty continuation appended but trimmed to original
        assertEquals("1000", result[0][2])
        assertEquals("50000", result[0][3])
    }

    // ─── 4. Multiple consecutive continuation rows ────────────────────────────

    @Test
    fun `multiple consecutive continuation rows all merge into one parent`() {
        val rows = listOf(
            listOf("01.03.2024", "Line one", "500", "10000"),
            listOf("", "Line two", "", ""),
            listOf("", "Line three", "", ""),
        )
        val result = extractor.mergeMultiLineRows(rows)

        assertEquals(1, result.size)
        assertEquals("01.03.2024", result[0][0])
        assertEquals("Line one Line two Line three", result[0][1])
        assertEquals("500", result[0][2])
    }

    // ─── 5. Cell-count mismatch — continuation has MORE cells than parent ─────

    @Test
    fun `continuation row with more cells than parent pads parent`() {
        // Parent has 3 cells, continuation has 5
        val rows = listOf(
            listOf("01.03.2024", "Desc", "1000"),
            listOf("", "extra1", "extra2", "extra3", "extra4"),
        )
        val result = extractor.mergeMultiLineRows(rows)

        assertEquals(1, result.size)
        // Parent was padded to 5 cells then continuation merged
        assertEquals(5, result[0].size)
        assertEquals("01.03.2024", result[0][0])
        assertEquals("Desc extra1", result[0][1])
        assertEquals("1000 extra2", result[0][2])
        assertEquals("extra3", result[0][3])
        assertEquals("extra4", result[0][4])
    }

    // ─── 6. Cell-count mismatch — continuation has FEWER cells than parent ────

    @Test
    fun `continuation row with fewer cells than parent only merges existing cells`() {
        // Parent has 4 cells, continuation has 2
        val rows = listOf(
            listOf("01.03.2024", "Description", "2000", "30000"),
            listOf("", "extra text"),
        )
        val result = extractor.mergeMultiLineRows(rows)

        assertEquals(1, result.size)
        assertEquals(4, result[0].size)
        assertEquals("01.03.2024", result[0][0])
        assertEquals("Description extra text", result[0][1])
        // Cells beyond continuation row's size are not touched
        assertEquals("2000", result[0][2])
        assertEquals("30000", result[0][3])
    }

    // ─── 7. First row has no date — treated as standalone (no crash) ──────────

    @Test
    fun `first row without a date is treated as standalone and does not crash`() {
        val rows = listOf(
            listOf("Header", "Column B", "Column C"),  // no date — becomes standalone
            listOf("01.03.2024", "Payment", "1500"),
        )
        val result = extractor.mergeMultiLineRows(rows)

        assertEquals(2, result.size)
        assertEquals(listOf("Header", "Column B", "Column C"), result[0])
        assertEquals(listOf("01.03.2024", "Payment", "1500"), result[1])
    }

    // ─── 8. Continuation before any date row attaches to standalone header ────

    @Test
    fun `continuation rows before any date row attach to the preceding standalone row`() {
        val rows = listOf(
            listOf("Header", "Col B"),  // standalone (no date)
            listOf("", "extra"),        // continuation — should merge into "Header" row
        )
        val result = extractor.mergeMultiLineRows(rows)

        assertEquals(1, result.size)
        assertEquals("Header", result[0][0])
        assertEquals("Col B extra", result[0][1])
    }

    // ─── 9. Mixed date and continuation rows in realistic sequence ────────────

    @Test
    fun `realistic BCC-style multi-line table is merged correctly`() {
        val rows = listOf(
            listOf("Date", "Description", "Amount", "Balance"),   // header, no date
            listOf("01.03.2024", "Supermarket purchase", "1200.00", "48800.00"),
            listOf("", "Almaty branch", "", ""),                   // continuation
            listOf("02.03.2024", "Utility bill", "5000.00", "43800.00"),
            listOf("03.03.2024", "Online shop", "700.00", "43100.00"),
            listOf("", "order #12345", "", ""),                    // continuation
            listOf("", "expedited delivery", "", ""),              // second continuation
        )
        val result = extractor.mergeMultiLineRows(rows)

        assertEquals(4, result.size)   // header + 3 logical transactions

        // Header row unchanged (no date)
        assertEquals(listOf("Date", "Description", "Amount", "Balance"), result[0])

        // First transaction: continuation merged
        assertEquals("01.03.2024", result[1][0])
        assertEquals("Supermarket purchase Almaty branch", result[1][1])
        assertEquals("1200.00", result[1][2])

        // Second transaction: no continuation
        assertEquals("02.03.2024", result[2][0])
        assertEquals("Utility bill", result[2][1])

        // Third transaction: two continuations merged
        assertEquals("03.03.2024", result[3][0])
        assertEquals("Online shop order #12345 expedited delivery", result[3][1])
    }

    // ─── 9b. Footer rows after MAX_CONTINUATION_ROWS are dropped ─────────────

    @Test
    fun `footer rows beyond MAX_CONTINUATION_ROWS are not merged into last transaction`() {
        val rows = listOf(
            listOf("01.03.2024", "Purchase", "1000", "50000"),
            listOf("", "cont 1", "", ""),       // continuation 1
            listOf("", "cont 2", "", ""),       // continuation 2
            listOf("", "cont 3", "", ""),       // continuation 3 (hits MAX)
            listOf("", "Total", "99999", ""),   // footer — should be dropped
            listOf("", "Ending balance", "", "100000"),  // footer — should be dropped
        )
        val result = extractor.mergeMultiLineRows(rows)

        assertEquals(1, result.size)
        assertEquals("01.03.2024", result[0][0])
        assertEquals("Purchase cont 1 cont 2 cont 3", result[0][1])
        // Footer rows must NOT be merged
        assertFalse(result[0][1].contains("Total"))
        assertFalse(result[0].any { it.contains("Ending balance") })
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // stripPageHeaders tests
    // ═══════════════════════════════════════════════════════════════════════════

    // ─── 10. First page keeps all rows unchanged ────────────────────────────

    @Test
    fun `stripPageHeaders keeps all rows on first page`() {
        val rows = listOf(
            listOf("Header", "Col A", "Col B"),
            listOf("01.03.2024", "Purchase", "1000"),
            listOf("02.03.2024", "Transfer", "2000"),
        )
        val result = extractor.stripPageHeaders(rows, isFirstPage = true)

        assertEquals(rows, result)
    }

    // ─── 11. Continuation page strips header rows ───────────────────────────

    @Test
    fun `stripPageHeaders strips header rows on continuation page`() {
        val rows = listOf(
            listOf("Operation date", "Description", "Amount", "Balance"),  // repeated header
            listOf("06.03.2024", "Online purchase", "-3000.00", "47000.00"),
            listOf("07.03.2024", "Salary", "200000.00", "247000.00"),
        )
        val result = extractor.stripPageHeaders(rows, isFirstPage = false)

        assertEquals(2, result.size)
        assertEquals("06.03.2024", result[0][0])
        assertEquals("07.03.2024", result[1][0])
    }

    // ─── 12. Footer-only page returns empty ─────────────────────────────────

    @Test
    fun `stripPageHeaders returns empty for footer-only page`() {
        val rows = listOf(
            listOf("Vice President of Retail Business Development", "", "", ""),
            listOf("Kairzhanova Zh.K.", "", "", ""),
            listOf("The QR code contains a web link", "", "", ""),
        )
        val result = extractor.stripPageHeaders(rows, isFirstPage = false)

        assertTrue(result.isEmpty())
    }

    // ─── 13. Page with only date rows passes through unchanged ──────────────

    @Test
    fun `stripPageHeaders keeps all rows when no pre-header exists`() {
        val rows = listOf(
            listOf("01.03.2024", "Purchase", "1000"),
            listOf("02.03.2024", "Transfer", "2000"),
        )
        val result = extractor.stripPageHeaders(rows, isFirstPage = false)

        assertEquals(rows, result)
    }

    // ─── 14. Continuation page preserves continuation rows after date rows ──

    @Test
    fun `stripPageHeaders on continuation page keeps continuation rows after date rows`() {
        val rows = listOf(
            listOf("Operation date", "Description", "Amount"),  // repeated header
            listOf("06.03.2024", "Long description", "3000"),
            listOf("", "continued text", ""),                    // multi-line continuation
            listOf("07.03.2024", "Another tx", "5000"),
        )
        val result = extractor.stripPageHeaders(rows, isFirstPage = false)

        assertEquals(3, result.size)
        assertEquals("06.03.2024", result[0][0])
        assertEquals("", result[1][0])  // continuation preserved
        assertEquals("07.03.2024", result[2][0])
    }

    // ─── 15. BCC 3-page scenario — repeated headers don't corrupt txns ──────

    @Test
    fun `BCC 3-page scenario - repeated headers do not corrupt transactions`() {
        val header = listOf("Operation date", "Reflection date", "Description", "Amount", "Balance")

        val page1 = listOf(
            header,
            listOf("2024-10-18", "2024-10-18", "Salary deposit", "107061.00", "107061.00"),
            listOf("", "", "АО Финансовый центр", "", ""),  // continuation
            listOf("2024-10-19", "2024-10-19", "Transfer", "-100000.00", "7061.00"),
            listOf("2024-10-27", "2024-10-27", "Top-up", "90000.00", "97061.00"),
        )
        val page2 = listOf(
            header,  // REPEATED header — must be stripped
            listOf("2024-10-27", "2024-10-27", "Transfer", "-95000.00", "2061.00"),
            listOf("2024-10-31", "2024-10-31", "Salary deposit", "107061.00", "109122.00"),
            listOf("", "", "АО Финансовый центр", "", ""),  // continuation
            listOf("2024-11-01", "2024-11-01", "Transfer", "-105000.00", "4122.00"),
        )
        val page3 = listOf(
            header,  // REPEATED header — must be stripped
            listOf("2024-11-27", "2024-11-27", "Salary deposit", "107061.00", "111183.00"),
            listOf("", "", "АО Финансовый центр", "", ""),  // continuation
        )

        // Simulate extractTable: stripPageHeaders per page, then merge
        val allRows =
            extractor.stripPageHeaders(page1, isFirstPage = true) +
            extractor.stripPageHeaders(page2, isFirstPage = false) +
            extractor.stripPageHeaders(page3, isFirstPage = false)

        val merged = extractor.mergeMultiLineRows(allRows)

        // Header should appear exactly once (from page 1)
        val headerCount = merged.count { it[0] == "Operation date" }
        assertEquals(1, headerCount)

        // All 7 transactions should survive (3 from page1, 3 from page2, 1 from page3)
        val dateRows = merged.filter { row ->
            val cell = row.firstOrNull()?.trim().orEmpty()
            cell.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))
        }
        assertEquals(7, dateRows.size)

        // Verify multi-line descriptions merged correctly (not corrupted by headers)
        val salaryRows = dateRows.filter { it[2].contains("Salary") }
        assertEquals(3, salaryRows.size)
        salaryRows.forEach { row ->
            assertTrue(
                "Salary row description should contain continuation: ${row[2]}",
                row[2].contains("АО Финансовый центр"),
            )
        }
    }

    // ─── 16. Empty input to stripPageHeaders ────────────────────────────────

    @Test
    fun `stripPageHeaders handles empty input`() {
        assertTrue(extractor.stripPageHeaders(emptyList(), isFirstPage = true).isEmpty())
        assertTrue(extractor.stripPageHeaders(emptyList(), isFirstPage = false).isEmpty())
    }
}
