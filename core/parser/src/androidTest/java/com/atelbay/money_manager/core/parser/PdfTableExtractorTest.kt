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
}
