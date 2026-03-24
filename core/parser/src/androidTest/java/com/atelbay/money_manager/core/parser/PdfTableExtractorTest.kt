package com.atelbay.money_manager.core.parser

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

/**
 * Instrumented tests for [PdfTableExtractor].
 *
 * Uses Android PdfBox (com.tom_roush.pdfbox) to create PDF fixtures and test
 * table extraction with the real PdfBox runtime.
 */
@RunWith(AndroidJUnit4::class)
class PdfTableExtractorTest {

    private lateinit var extractor: PdfTableExtractor

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        PDFBoxResourceLoader.init(context)
        val pdfTextExtractor = PdfTextExtractor(context)
        extractor = PdfTableExtractor(pdfTextExtractor)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

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

    @Test
    fun extractTable_returnsCorrectRowsForSimple4ColumnTable() {
        val data = listOf(
            listOf("Date", "Description", "Amount", "Balance"),
            listOf("01.01.26", "Supermarket", "5000", "45000"),
            listOf("02.01.26", "Pharmacy", "1200", "43800"),
        )
        val bytes = buildFourColumnTable(data)

        val result = extractor.extractTable(bytes)

        assertEquals("Should extract 3 rows", 3, result.size)
        result.forEach { row ->
            assertEquals("Each row must have 4 columns", 4, row.size)
        }
    }

    @Test
    fun extractTableOrNull_returnsNonNullWithHeaderAndDataRows() {
        val data = listOf(
            listOf("Date", "Operation", "Amount", "Balance"),
            listOf("05.03.26", "Purchase", "3500", "100000"),
            listOf("06.03.26", "Transfer", "10000", "90000"),
        )
        val bytes = buildFourColumnTable(data)

        val result = extractor.extractTableOrNull(bytes)

        assertNotNull("extractTableOrNull must return non-null for 3 rows", result)
        assertTrue("Result must contain at least 2 rows", result!!.size >= 2)
        assertEquals(4, result.first().size)
    }

    @Test
    fun extractTable_onSingleLineTextDoesNotCrash() {
        val singleLineRows = listOf(
            listOf(Triple(50f, 700f, "Nobankstatementhere")),
        )
        val bytes = buildPdf(singleLineRows)

        val result = extractor.extractTable(bytes)
        assertNotNull("extractTable must never return null", result)
    }

    @Test
    fun extractTableOrNull_returnsNullWhenOnlyOneRow() {
        val singleRowPdf = buildPdf(
            listOf(
                listOf(
                    Triple(50f, 700f, "Col1"),
                    Triple(200f, 700f, "Col2"),
                ),
            ),
        )

        val result = extractor.extractTableOrNull(singleRowPdf)
        assertNull("extractTableOrNull must return null when fewer than 2 rows", result)
    }

    @Test
    fun extractTable_onEmptyByteArrayReturnsEmptyList() {
        val result = extractor.extractTable(ByteArray(0))
        assertTrue("extractTable must return empty list for empty bytes", result.isEmpty())
    }

    @Test
    fun extractTableOrNull_onEmptyByteArrayReturnsNull() {
        val result = extractor.extractTableOrNull(ByteArray(0))
        assertNull("extractTableOrNull must return null for empty bytes", result)
    }

    @Test
    fun extractTable_onInvalidBytesReturnsEmptyList() {
        val garbage = byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04)
        val result = extractor.extractTable(garbage)
        assertTrue("extractTable must return empty list for non-PDF bytes", result.isEmpty())
    }

    @Test
    fun extractTable_insertsSpacesBetweenWordsWithinCell() {
        // Two words in the same column with a small visual gap (word spacing)
        // and a second column far away (column spacing)
        // Font size 10, Helvetica: widthOfSpace ≈ 2.78, "Hello" ≈ 25pt wide
        // Gap of ~10pt between "Hello" and "World" = word gap (< column threshold)
        // Gap of ~120pt to "Other" = column gap
        val rows = listOf(
            listOf(
                Triple(50f, 700f, "Hello"),
                Triple(80f, 700f, "World"),
                Triple(250f, 700f, "Other"),
            ),
            listOf(
                Triple(50f, 680f, "Foo"),
                Triple(72f, 680f, "Bar"),
                Triple(250f, 680f, "Baz"),
            ),
            listOf(
                Triple(50f, 660f, "One"),
                Triple(72f, 660f, "Two"),
                Triple(250f, 660f, "Three"),
            ),
        )
        val bytes = buildPdf(rows)

        val result = extractor.extractTable(bytes)

        assertTrue("Should have at least 3 rows", result.size >= 3)
        assertEquals("Should detect 2 columns, got: ${result.first()}", 2, result.first().size)
        assertTrue(
            "First cell should contain space between words: '${result[0][0]}'",
            result[0][0].contains(" "),
        )
    }

    @Test
    fun extractTable_detects8ColumnsInDenseTable() {
        // Mimic Halyk-like 8-column layout with tight spacing
        val colX = listOf(30f, 90f, 160f, 260f, 320f, 370f, 430f, 500f)
        val dataRows = listOf(
            listOf("Date1", "Date2", "Description", "Amount", "CCY", "Credit", "Debit", "Card"),
            listOf("01.03", "01.03", "Purchase", "-5000", "KZT", "0", "-5000", "4874"),
            listOf("02.03", "02.03", "Transfer", "10000", "KZT", "10000", "0", "6291"),
            listOf("03.03", "03.03", "Payment", "-1200", "KZT", "0", "-1200", "4874"),
        )
        val rows = dataRows.mapIndexed { rowIndex, cols ->
            val y = 700f - rowIndex * 20f
            cols.mapIndexed { colIndex, text ->
                Triple(colX[colIndex], y, text)
            }
        }
        val bytes = buildPdf(rows)

        val result = extractor.extractTable(bytes)

        assertEquals("Should extract 4 rows", 4, result.size)
        assertEquals("Should detect 8 columns", 8, result.first().size)
        assertEquals("Date1", result[0][0])
        assertEquals("Description", result[0][2])
        assertEquals("Card", result[0][7])
    }

    @Test
    fun extractTable_preservesSpacesInSingleShowTextMultiWordCells() {
        // Real PDFs render multi-word text as a single showText call.
        // The extractor must insert spaces between words within the same cell.
        val colX = listOf(50f, 200f, 350f)
        val dataRows = listOf(
            listOf("Date", "Description", "Amount"),
            listOf("01.01.26", "Purchase at store", "5000"),
            listOf("02.01.26", "Transfer to John", "1200"),
            listOf("03.01.26", "Cash withdrawal", "800"),
        )
        val rows = dataRows.mapIndexed { rowIndex, cols ->
            val y = 700f - rowIndex * 20f
            cols.mapIndexed { colIndex, text ->
                Triple(colX[colIndex], y, text)
            }
        }
        val bytes = buildPdf(rows)

        val result = extractor.extractTable(bytes)

        assertEquals("Should extract 4 rows", 4, result.size)
        assertEquals("Should detect 3 columns", 3, result.first().size)
        // Multi-word cells must preserve spaces
        assertTrue(
            "Description cell should contain spaces: '${result[1][1]}'",
            result[1][1].contains(" "),
        )
        assertTrue(
            "Description cell should contain 'store': '${result[1][1]}'",
            result[1][1].contains("store"),
        )
        // Single-word cells should be intact
        assertEquals("5000", result[1][2])
        assertEquals("01.01.26", result[1][0])
    }

    @Test
    fun extractTable_metadataRowsDoNotCorruptColumnDetection() {
        // 2 full-width metadata rows + 4 structured data rows with 4 columns
        val colX = listOf(50f, 150f, 250f, 350f)
        val pdfRows = mutableListOf<List<Triple<Float, Float, String>>>()

        // Metadata rows (single text spanning the page)
        pdfRows.add(listOf(Triple(50f, 740f, "Halyk Savings Bank of Kazakhstan")))
        pdfRows.add(listOf(Triple(50f, 720f, "Statement for period 01.01-31.01")))

        // Structured data rows
        val dataRows = listOf(
            listOf("Date", "Operation", "Amount", "Balance"),
            listOf("01.01", "Purchase", "5000", "45000"),
            listOf("02.01", "Transfer", "1200", "43800"),
            listOf("03.01", "Payment", "800", "43000"),
        )
        for ((rowIndex, cols) in dataRows.withIndex()) {
            val y = 700f - rowIndex * 20f
            pdfRows.add(
                cols.mapIndexed { colIndex, text ->
                    Triple(colX[colIndex], y, text)
                },
            )
        }

        val bytes = buildPdf(pdfRows)
        val result = extractor.extractTable(bytes)

        assertEquals("Should extract 6 rows total", 6, result.size)
        // Data rows should have 4 columns
        val dataResult = result.drop(2) // skip metadata
        for (row in dataResult) {
            assertEquals("Data row should have 4 columns", 4, row.size)
        }
        assertEquals("Date", dataResult[0][0])
        assertEquals("Balance", dataResult[0][3])
    }
}
