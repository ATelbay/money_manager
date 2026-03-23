package com.atelbay.money_manager.core.parser

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import timber.log.Timber
import java.io.ByteArrayInputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class PdfTableExtractor @Inject constructor(
    private val pdfTextExtractor: PdfTextExtractor,
) {

    companion object {
        private const val Y_TOLERANCE = 2f
        private const val X_GAP_THRESHOLD = 10f
    }

    /**
     * Extracts a table from the PDF bytes. Returns an empty list on failure or if no table found.
     */
    fun extractTable(bytes: ByteArray): List<List<String>> {
        return try {
            pdfTextExtractor.ensureInitialized()
            ByteArrayInputStream(bytes).use { stream ->
                PDDocument.load(stream).use { document ->
                    val allRows = mutableListOf<List<String>>()
                    for (pageIndex in 0 until document.numberOfPages) {
                        val stripper = TableTextStripper()
                        stripper.startPage = pageIndex + 1
                        stripper.endPage = pageIndex + 1
                        stripper.getText(document)
                        allRows.addAll(stripper.buildTable())
                    }
                    allRows
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to extract table from PDF (%d bytes)", bytes.size)
            emptyList()
        }
    }

    /**
     * Extracts a table from the PDF bytes. Returns null if fewer than 2 rows found.
     */
    fun extractTableOrNull(bytes: ByteArray): List<List<String>>? {
        val table = extractTable(bytes)
        return if (table.size < 2) null else table
    }

    private inner class TableTextStripper : PDFTextStripper() {

        private val positions = mutableListOf<TextPosition>()

        init {
            sortByPosition = true
        }

        override fun writeString(text: String, textPositions: List<TextPosition>) {
            positions.addAll(textPositions)
        }

        fun buildTable(): List<List<String>> {
            if (positions.isEmpty()) return emptyList()

            // Group positions by Y coordinate (row) with tolerance
            val rows = mutableListOf<MutableList<TextPosition>>()
            for (pos in positions) {
                val existingRow = rows.find { row ->
                    abs(row.first().yDirAdj - pos.yDirAdj) <= Y_TOLERANCE
                }
                if (existingRow != null) {
                    existingRow.add(pos)
                } else {
                    rows.add(mutableListOf(pos))
                }
            }

            // Sort rows top-to-bottom
            rows.sortBy { it.first().yDirAdj }

            if (rows.isEmpty()) return emptyList()

            // Detect column boundaries from the first content row using sorted X gaps
            val allXPositions = rows.flatMap { row -> row.map { it.xDirAdj } }.sorted()
            val columnBoundaries = mutableListOf<Float>()
            var prevX = allXPositions.firstOrNull() ?: return emptyList()
            for (x in allXPositions.drop(1)) {
                if (x - prevX > X_GAP_THRESHOLD) {
                    columnBoundaries.add((prevX + x) / 2f)
                }
                prevX = x
            }

            // Build table: assign each text position to a column
            return rows.map { row ->
                val colCount = columnBoundaries.size + 1
                val cells = Array(colCount) { StringBuilder() }
                val sortedRow = row.sortedBy { it.xDirAdj }
                for (pos in sortedRow) {
                    val colIndex = columnBoundaries.indexOfFirst { pos.xDirAdj < it }
                        .let { if (it == -1) columnBoundaries.size else it }
                    cells[colIndex].append(pos.unicode)
                }
                cells.map { it.toString().trim() }
            }
        }
    }
}
