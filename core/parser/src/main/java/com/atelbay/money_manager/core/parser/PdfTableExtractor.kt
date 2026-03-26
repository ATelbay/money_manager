package com.atelbay.money_manager.core.parser

import androidx.annotation.VisibleForTesting
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
        private const val CLUSTER_TOLERANCE = 15f
        private const val SPACE_WIDTH_FACTOR = 0.3f
        private const val COLUMN_GAP_SPACE_FACTOR = 3f
        private const val MIN_DATE_ROWS = 3
        private const val MAX_CONTINUATION_ROWS = 3
        // Anchored at start: a row that starts with a date token begins a new logical transaction.
        // Trailing [-./]? allows partial matches like "01.03" before the year part.
        private val DATE_ROW_PATTERN = Regex("^${ParserPatterns.DATE_CORE}[-./]?")
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
                        val pageRows = stripper.buildTable()
                    allRows.addAll(stripPageHeaders(pageRows, isFirstPage = pageIndex == 0))
                    }
                    mergeMultiLineRows(allRows)
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

    /**
     * On continuation pages (not the first page), strips rows that appear before the
     * first date-starting row. These are repeated column headers that PDF renderers
     * place at the top of every page.
     *
     * On the first page, all rows are kept — the original header is preserved and
     * handled by [TableStatementParser] via skipHeaderRows.
     *
     * Footer-only pages (no date rows) return empty to prevent their content from
     * merging into the previous page's last transaction.
     */
    @VisibleForTesting
    internal fun stripPageHeaders(
        pageRows: List<List<String>>,
        isFirstPage: Boolean,
    ): List<List<String>> {
        if (isFirstPage || pageRows.isEmpty()) return pageRows

        val firstDateIndex = pageRows.indexOfFirst { row ->
            val firstCell = row.firstOrNull()?.trim().orEmpty()
            DATE_ROW_PATTERN.containsMatchIn(firstCell)
        }

        // No date rows — footer-only page (signatures, QR). Discard to prevent
        // merging into previous page's last transaction.
        if (firstDateIndex == -1) return emptyList()

        return pageRows.subList(firstDateIndex, pageRows.size)
    }

    /**
     * Merges continuation rows (rows whose first cell does not start with a date) into
     * the preceding logical row by appending cell text with a space separator.
     *
     * Heuristic: a row that starts with a date-like token (DD-MM or YYYY-MM prefix) begins a new
     * logical transaction; any row that doesn't is treated as a wrapped continuation of the
     * previous one.
     *
     * Cell-count padding edge case: if a continuation row has MORE cells than the parent, the
     * parent is padded with empty strings so indices stay aligned. If it has FEWER cells, only
     * the cells that exist in the continuation row are merged.
     */
    @VisibleForTesting
    internal fun mergeMultiLineRows(rows: List<List<String>>): List<List<String>> {
        val result = mutableListOf<MutableList<String>>()
        val continuationCounts = mutableMapOf<Int, Int>()
        for (row in rows) {
            val firstCell = row.firstOrNull()?.trim().orEmpty()
            if (DATE_ROW_PATTERN.containsMatchIn(firstCell)) {
                // New logical row — starts with a date-like token
                result.add(row.map { it }.toMutableList())
            } else if (result.isEmpty()) {
                // No parent yet (e.g. header rows before any date), treat as standalone
                result.add(row.map { it }.toMutableList())
            } else {
                val parentIndex = result.lastIndex
                val count = continuationCounts.getOrDefault(parentIndex, 0)
                if (count >= MAX_CONTINUATION_ROWS) {
                    // Likely a footer/summary row — don't merge into last transaction
                    continue
                }
                continuationCounts[parentIndex] = count + 1
                val parent = result[parentIndex]
                // Pad parent if continuation row has more cells
                if (row.size > parent.size) {
                    repeat(row.size - parent.size) { parent.add("") }
                }
                // Append continuation text to each cell up to continuation row's size
                for (i in row.indices) {
                    parent[i] = (parent[i] + " " + row[i]).trim()
                }
            }
        }
        // Final trim pass on all cells
        return result.map { row -> row.map { it.trim() } }
    }

    /**
     * Detects column boundaries by finding inter-glyph gaps per row
     * and voting across rows for consistent boundary positions.
     */
    private fun detectColumnBoundaries(
        rows: List<MutableList<TextPosition>>,
    ): List<Float> {
        // Filter to transaction-like rows for voting — metadata rows have different
        // column structures and dilute transaction-column gap votes below threshold
        val effectiveRows = rows.filter { rowStartsWithDate(it) }
            .takeIf { it.size >= MIN_DATE_ROWS }
            ?: rows

        // Step 1: Collect per-row gap midpoints
        val allGapMidpoints = mutableListOf<Float>()
        for (row in effectiveRows) {
            if (row.size < 2) continue
            val sorted = row.sortedBy { it.xDirAdj }
            for (i in 1 until sorted.size) {
                val prev = sorted[i - 1]
                val curr = sorted[i]
                val prevRightEdge = prev.xDirAdj + prev.width
                val gap = curr.xDirAdj - prevRightEdge
                val spaceWidth = curr.widthOfSpace
                val threshold = if (spaceWidth > 0f) {
                    maxOf(spaceWidth * COLUMN_GAP_SPACE_FACTOR, X_GAP_THRESHOLD)
                } else {
                    X_GAP_THRESHOLD
                }
                if (gap > threshold) {
                    allGapMidpoints.add((prevRightEdge + curr.xDirAdj) / 2f)
                }
            }
        }

        if (allGapMidpoints.isEmpty()) return emptyList()

        // Step 2: Cluster midpoints
        allGapMidpoints.sort()
        val clusters = mutableListOf<MutableList<Float>>()
        for (mp in allGapMidpoints) {
            val lastCluster = clusters.lastOrNull()
            if (lastCluster != null && mp - lastCluster.last() <= CLUSTER_TOLERANCE) {
                lastCluster.add(mp)
            } else {
                clusters.add(mutableListOf(mp))
            }
        }

        // Step 3: Vote — accept clusters with enough support
        val minVotes = (effectiveRows.size / 3).coerceAtLeast(1)
        val boundaries = clusters
            .filter { it.size >= minVotes }
            .map { cluster -> cluster.average().toFloat() }
            .sorted()

        Timber.d("detectColumnBoundaries: %d clusters, minVotes=%d, %d boundaries from %d rows (%d date-filtered)",
            clusters.size, minVotes, boundaries.size, rows.size, effectiveRows.size)
        if (boundaries.isNotEmpty()) return boundaries

        // Fallback: old global gap approach for backward compatibility
        Timber.d("detectColumnBoundaries: voting found 0 boundaries, using fallback global gap approach")
        val allXPositions = rows.flatMap { row -> row.map { it.xDirAdj } }.sorted()
        val fallback = mutableListOf<Float>()
        var prevX = allXPositions.firstOrNull() ?: return emptyList()
        for (x in allXPositions.drop(1)) {
            if (x - prevX > X_GAP_THRESHOLD) {
                fallback.add((prevX + x) / 2f)
            }
            prevX = x
        }
        return fallback
    }

    /**
     * Checks if a row of raw glyphs starts with a date-like pattern (e.g., "2024-12-31", "01.03").
     * Used to filter metadata rows out of column boundary voting.
     */
    private fun rowStartsWithDate(row: List<TextPosition>): Boolean {
        if (row.isEmpty()) return false
        val sorted = row.sortedBy { it.xDirAdj }
        val prefix = buildString {
            for (pos in sorted) {
                append(pos.unicode)
                if (length >= 12) break
            }
        }
        return DATE_ROW_PATTERN.containsMatchIn(prefix)
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

            // Detect column boundaries via per-row gap detection + cross-row voting
            val columnBoundaries = detectColumnBoundaries(rows)

            Timber.d("buildTable: %d rows, %d columns", rows.size, columnBoundaries.size + 1)

            // Build table: assign each text position to a column, inserting spaces
            return rows.map { row ->
                val colCount = columnBoundaries.size + 1
                val cells = Array(colCount) { StringBuilder() }
                val lastPosInCol = arrayOfNulls<TextPosition>(colCount)
                val sortedRow = row.sortedBy { it.xDirAdj }
                for (pos in sortedRow) {
                    val colIndex = columnBoundaries.indexOfFirst { pos.xDirAdj < it }
                        .let { if (it == -1) columnBoundaries.size else it }
                    val prev = lastPosInCol[colIndex]
                    if (prev != null) {
                        val gap = pos.xDirAdj - (prev.xDirAdj + prev.width)
                        val spaceWidth = pos.widthOfSpace
                        val threshold = if (spaceWidth > 0f) {
                            spaceWidth * SPACE_WIDTH_FACTOR
                        } else {
                            // Fallback: estimate from character width (widthOfSpace=0 in some fonts)
                            prev.width * SPACE_WIDTH_FACTOR
                        }
                        if (gap > threshold) {
                            cells[colIndex].append(' ')
                        }
                    }
                    cells[colIndex].append(pos.unicode)
                    lastPosInCol[colIndex] = pos
                }
                cells.map { it.toString().trim() }
            }
        }
    }
}
