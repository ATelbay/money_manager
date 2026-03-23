package com.atelbay.money_manager.core.parser

import io.mockk.mockk
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.io.ByteArrayOutputStream

/**
 * Unit tests for [PdfTableExtractor].
 *
 * Uses JVM PdfBox (org.apache.pdfbox) to create PDF fixtures programmatically.
 * PdfTextExtractor is injected as a relaxed mock (ensureInitialized() is a no-op).
 *
 * NOTE: Tests are @Ignore because PdfTableExtractor extends com.tom_roush.pdfbox.text.PDFTextStripper
 * which requires Android-specific LegacyPDFStreamEngine at class-load time. These tests need
 * instrumented (androidTest) execution or Robolectric to function.
 */
@Ignore("Requires Android PdfBox runtime — move to androidTest or use Robolectric")
class PdfTableExtractorTest {

    private val mockPdfTextExtractor: PdfTextExtractor = mockk(relaxed = true)
    private lateinit var extractor: PdfTableExtractor

    @Before
    fun setUp() {
        extractor = PdfTableExtractor(mockPdfTextExtractor)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates a minimal PDF with a single page containing [lines] of text.
     * Each string in a line is placed at a fixed X offset (column positions)
     * and the same Y coordinate, simulating a table row.
     *
     * @param rows  List of rows; each row is a list of (x, y, text) triples.
     */
    private fun buildPdf(rows: List<List<Triple<Float, Float, String>>>): ByteArray {
        val doc = PDDocument()
        val page = PDPage(PDRectangle.A4)
        doc.addPage(page)

        val font = PDType1Font.HELVETICA
        PDPageContentStream(doc, page).use { cs ->
            for (row in rows) {
                for ((x, y, text) in row) {
                    cs.beginText()
                    cs.setFont(font, 10f)
                    cs.newLineAtOffset(x, y)
                    cs.showText(text)
                    cs.endText()
                }
            }
        }

        val out = ByteArrayOutputStream()
        doc.save(out)
        doc.close()
        return out.toByteArray()
    }

    /**
     * Builds a simple 4-column PDF table.
     * Rows are spaced 20pt apart vertically; columns at x = 50, 150, 250, 350.
     */
    private fun buildFourColumnTable(
        dataRows: List<List<String>>,
        startY: Float = 700f,
    ): ByteArray {
        val colX = listOf(50f, 150f, 250f, 350f)
        val rows = dataRows.mapIndexed { rowIndex, cols ->
            val y = startY - rowIndex * 20f
            cols.mapIndexed { colIndex, text ->
                Triple(colX[colIndex], y, text)
            }
        }
        return buildPdf(rows)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 1. Simple 4-column table extraction.
     *
     * Verifies that extractTable returns the correct number of rows and that
     * cell values from each column are present in the output.
     */
    @Test
    fun `extractTable returns correct rows for simple 4-column table`() {
        val data = listOf(
            listOf("Date", "Description", "Amount", "Balance"),
            listOf("01.01.26", "Supermarket", "5000", "45000"),
            listOf("02.01.26", "Pharmacy", "1200", "43800"),
        )
        val bytes = buildFourColumnTable(data)

        val result = extractor.extractTable(bytes)

        assertEquals("Should extract 3 rows", 3, result.size)
        // Each extracted row must have 4 cells
        result.forEach { row ->
            assertEquals("Each row must have 4 columns", 4, row.size)
        }
    }

    /**
     * 2. Table with header row.
     *
     * The first row is a header; remaining rows are data. Validates that
     * extractTableOrNull returns a non-null result and that the header values
     * are present in the first row.
     */
    @Test
    fun `extractTableOrNull returns non-null result with header and data rows`() {
        val data = listOf(
            listOf("Дата", "Операция", "Сумма", "Остаток"),
            listOf("05.03.26", "Покупка", "3500", "100000"),
            listOf("06.03.26", "Перевод", "10000", "90000"),
        )
        val bytes = buildFourColumnTable(data)

        val result = extractor.extractTableOrNull(bytes)

        assertNotNull("extractTableOrNull must return non-null for 3 rows", result)
        assertTrue("Result must contain at least 2 rows", result!!.size >= 2)

        val headerRow = result.first()
        assertEquals(4, headerRow.size)
    }

    /**
     * 3. PDF with no table structure → empty result.
     *
     * A single line of text (no columns separated by a gap ≥ X_GAP_THRESHOLD)
     * should produce either an empty list or a single-row result with one cell,
     * and extractTableOrNull should return null (fewer than 2 rows that qualify
     * as multi-column).
     */
    @Test
    fun `extractTable on single-line text returns result without crashing`() {
        // Single line, all characters close together — no column gaps
        val singleLineRows = listOf(
            listOf(Triple(50f, 700f, "Nobankstatementhere")),
        )
        val bytes = buildPdf(singleLineRows)

        // Must not throw; result may be empty or contain one row
        val result = extractor.extractTable(bytes)
        assertNotNull("extractTable must never return null", result)
    }

    @Test
    fun `extractTableOrNull returns null when only one row is extracted`() {
        // One row with a couple of columns — still fewer than 2 rows total
        val singleRowPdf = buildPdf(
            listOf(
                listOf(
                    Triple(50f, 700f, "Col1"),
                    Triple(200f, 700f, "Col2"),
                ),
            ),
        )

        val result = extractor.extractTableOrNull(singleRowPdf)
        assertNull(
            "extractTableOrNull must return null when fewer than 2 rows are found",
            result,
        )
    }

    /**
     * 4. Empty / null input handling.
     *
     * Passing an empty ByteArray must return an empty list (caught internally)
     * without throwing an exception.
     */
    @Test
    fun `extractTable on empty ByteArray returns empty list`() {
        val result = extractor.extractTable(ByteArray(0))

        assertTrue(
            "extractTable must return empty list for empty bytes",
            result.isEmpty(),
        )
    }

    @Test
    fun `extractTableOrNull on empty ByteArray returns null`() {
        val result = extractor.extractTableOrNull(ByteArray(0))
        assertNull(
            "extractTableOrNull must return null for empty bytes",
            result,
        )
    }

    @Test
    fun `extractTable on invalid bytes returns empty list`() {
        val garbage = byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04)

        val result = extractor.extractTable(garbage)

        assertTrue(
            "extractTable must return empty list for non-PDF bytes",
            result.isEmpty(),
        )
    }
}
