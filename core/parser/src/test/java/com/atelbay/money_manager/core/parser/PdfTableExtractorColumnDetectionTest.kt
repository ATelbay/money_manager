package com.atelbay.money_manager.core.parser

import com.atelbay.money_manager.core.parser.PdfTableExtractor.GlyphInfo
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for header-anchored column detection in [PdfTableExtractor].
 *
 * Uses [GlyphInfo] directly — no PdfBox runtime needed (pure JVM tests).
 */
class PdfTableExtractorColumnDetectionTest {

    private lateinit var extractor: PdfTableExtractor

    @Before
    fun setUp() {
        extractor = PdfTableExtractor(mockk(relaxed = true))
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    /**
     * Builds a row of [GlyphInfo] from a list of "words" at specified X positions.
     * Each character gets a fixed width and spaceWidth.
     */
    private fun buildGlyphRow(
        words: List<Pair<Float, String>>,
        charWidth: Float = 7f,
        spaceWidth: Float = 4f,
    ): List<GlyphInfo> {
        val glyphs = mutableListOf<GlyphInfo>()
        for ((startX, word) in words) {
            for ((i, ch) in word.withIndex()) {
                glyphs.add(
                    GlyphInfo(
                        x = startX + i * charWidth,
                        width = charWidth,
                        spaceWidth = spaceWidth,
                        char = ch.toString(),
                    ),
                )
            }
        }
        return glyphs
    }

    // ─── detectHeaderRow ─────────────────────────────────────────────────────

    @Test
    fun `Freedom header with 5 keywords is detected`() {
        // "Дата   Сумма   Валюта   Операция   Детали"
        val headerRow = buildGlyphRow(
            listOf(
                0f to "Дата",
                80f to "Сумма",
                160f to "Валюта",
                240f to "Операция",
                340f to "Детали",
            ),
        )
        val dataRow = buildGlyphRow(listOf(0f to "01.03.2024", 80f to "5000", 160f to "KZT"))
        val rows = listOf(headerRow, dataRow)

        val index = extractor.detectHeaderRow(rows)
        assertEquals(0, index)
    }

    @Test
    fun `Forte header with 4 keywords is detected`() {
        val headerRow = buildGlyphRow(
            listOf(
                0f to "Дата",
                80f to "Сумма",
                160f to "Операция",
                260f to "Детали",
            ),
        )
        val rows = listOf(headerRow)

        val index = extractor.detectHeaderRow(rows)
        assertEquals(0, index)
    }

    @Test
    fun `English header is detected`() {
        val headerRow = buildGlyphRow(
            listOf(
                0f to "Date",
                80f to "Amount",
                160f to "Currency",
                260f to "Description",
            ),
        )
        val rows = listOf(headerRow)

        val index = extractor.detectHeaderRow(rows)
        assertEquals(0, index)
    }

    @Test
    fun `row without keywords returns -1`() {
        val metadataRow = buildGlyphRow(listOf(0f to "АО Kaspi Bank", 200f to "БИН 123456"))
        val rows = listOf(metadataRow)

        val index = extractor.detectHeaderRow(rows)
        assertEquals(-1, index)
    }

    @Test
    fun `empty rows return -1`() {
        val index = extractor.detectHeaderRow(emptyList())
        assertEquals(-1, index)
    }

    @Test
    fun `header on row 2 is detected when preceded by metadata`() {
        val metadataRow = buildGlyphRow(listOf(0f to "Выписка по счету"))
        val blankRow = buildGlyphRow(listOf(0f to "Период: 01.01-31.01"))
        val headerRow = buildGlyphRow(
            listOf(0f to "Дата", 80f to "Сумма", 160f to "Описание"),
        )
        val rows = listOf(metadataRow, blankRow, headerRow)

        val index = extractor.detectHeaderRow(rows)
        assertEquals(2, index)
    }

    @Test
    fun `single keyword row is not detected as header`() {
        val row = buildGlyphRow(listOf(0f to "Дата выписки: 01.03.2024"))
        val rows = listOf(row)

        val index = extractor.detectHeaderRow(rows)
        assertEquals(-1, index)
    }

    @Test
    fun `two keyword metadata line is not detected as header`() {
        // "Дата выписки: остаток 500,000 KZT" — contains "дата" and "остаток" but is metadata
        val row = buildGlyphRow(
            listOf(0f to "Дата", 80f to "выписки:", 180f to "остаток", 300f to "500,000"),
        )
        val rows = listOf(row)

        val index = extractor.detectHeaderRow(rows)
        assertEquals(-1, index)
    }

    // ─── detectColumnBoundariesFromHeader ────────────────────────────────────

    @Test
    fun `Freedom-like header produces 4 boundaries`() {
        // 5 columns: Дата | Сумма | Валюта | Операция | Детали
        // Words at X=0, 80, 160, 240, 340 with charWidth=7, spaceWidth=4
        // Gaps between words: 80-28=52, 160-115=45, 240-202=38, 340-296=44
        // All >> spaceWidth*2=8, so 4 boundaries
        val header = buildGlyphRow(
            listOf(
                0f to "Дата",
                80f to "Сумма",
                160f to "Валюта",
                240f to "Операция",
                340f to "Детали",
            ),
        )
        val boundaries = extractor.detectColumnBoundariesFromHeader(header)
        assertEquals(4, boundaries.size)
    }

    @Test
    fun `Forte-like header produces 3 boundaries`() {
        val header = buildGlyphRow(
            listOf(
                0f to "Дата",
                80f to "Сумма",
                160f to "Операция",
                280f to "Детали",
            ),
        )
        val boundaries = extractor.detectColumnBoundariesFromHeader(header)
        assertEquals(3, boundaries.size)
    }

    @Test
    fun `multi-word header does not split within compound name`() {
        // "Дата операции" should be one word group (charWidth=7, gap between words ~ spaceWidth)
        // Use small gap (=spaceWidth) between "Дата" and "операции" → same column
        // Then large gap before "Сумма"
        val glyphs = mutableListOf<GlyphInfo>()
        val charWidth = 7f
        val spaceWidth = 4f

        // "Дата" at x=0
        for ((i, ch) in "Дата".withIndex()) {
            glyphs.add(GlyphInfo(x = i * charWidth, width = charWidth, spaceWidth = spaceWidth, char = ch.toString()))
        }
        // "операции" right after with only spaceWidth gap (within word threshold)
        val opStart = 4 * charWidth + spaceWidth // 28 + 4 = 32
        for ((i, ch) in "операции".withIndex()) {
            glyphs.add(GlyphInfo(x = opStart + i * charWidth, width = charWidth, spaceWidth = spaceWidth, char = ch.toString()))
        }
        // "Сумма" with large gap
        val sumStart = 150f
        for ((i, ch) in "Сумма".withIndex()) {
            glyphs.add(GlyphInfo(x = sumStart + i * charWidth, width = charWidth, spaceWidth = spaceWidth, char = ch.toString()))
        }

        val boundaries = extractor.detectColumnBoundariesFromHeader(glyphs)
        assertEquals("Should produce 1 boundary between 'Дата операции' and 'Сумма'", 1, boundaries.size)
    }

    @Test
    fun `empty glyph list returns empty boundaries`() {
        val boundaries = extractor.detectColumnBoundariesFromHeader(emptyList())
        assertTrue(boundaries.isEmpty())
    }

    @Test
    fun `single word returns empty boundaries`() {
        val header = buildGlyphRow(listOf(0f to "Дата"))
        val boundaries = extractor.detectColumnBoundariesFromHeader(header)
        assertTrue(boundaries.isEmpty())
    }

    @Test
    fun `boundaries are sorted left to right`() {
        val header = buildGlyphRow(
            listOf(
                0f to "Дата",
                100f to "Сумма",
                200f to "Валюта",
                300f to "Операция",
            ),
        )
        val boundaries = extractor.detectColumnBoundariesFromHeader(header)
        assertEquals(boundaries, boundaries.sorted())
    }

    @Test
    fun `boundary midpoints are between adjacent words`() {
        val header = buildGlyphRow(
            listOf(
                0f to "Дата",      // right edge = 0 + 4*7 = 28
                100f to "Сумма",   // left edge = 100
            ),
            charWidth = 7f,
        )
        val boundaries = extractor.detectColumnBoundariesFromHeader(header)
        assertEquals(1, boundaries.size)
        // Midpoint should be between 28 and 100 → 64
        val midpoint = boundaries[0]
        assertTrue("Midpoint $midpoint should be > 28", midpoint > 28f)
        assertTrue("Midpoint $midpoint should be < 100", midpoint < 100f)
    }
}
