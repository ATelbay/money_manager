package com.atelbay.money_manager.core.parser

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Validates table extraction quality to reject garbled results caused by
 * incorrect column boundary detection (e.g. "полнение счета", "тке KZ в").
 *
 * When quality is unacceptable, the waterfall parser falls back to regex.
 */
@Singleton
class TableQualityValidator @Inject constructor() {

    data class QualityResult(val isAcceptable: Boolean, val reason: String? = null)

    private companion object {
        /** Max fraction of text cells that may start with a lowercase letter. */
        const val FRAGMENT_THRESHOLD = 0.30f

        /** Max fraction of text cells that are ≤ 2 characters. */
        const val SHORT_CELL_THRESHOLD = 0.30f

        /** Min fraction of first-column cells that must contain a date. */
        const val DATE_PARSE_THRESHOLD = 0.50f

        /** Minimum number of data rows to run quality checks. */
        const val MIN_ROWS_FOR_CHECK = 3

        // Anchored at start: matches date tokens like "01.03.2024", "2024-12-31", "01/03".
        private val DATE_PATTERN = Regex("^${ParserPatterns.DATE_CORE}[-./]?\\d{0,4}")

        /** Cyrillic lowercase letter at start — signals a word fragment. */
        private val CYRILLIC_LOWERCASE_START = Regex("^[а-яё]")

        /** Latin lowercase letter at start — signals a word fragment. */
        private val LATIN_LOWERCASE_START = Regex("^[a-z]")
    }

    /**
     * Validates table quality. Skips header row (index 0) — checks data rows only.
     *
     * @param table The extracted table (first row is typically a header).
     * @return [QualityResult] with `isAcceptable = true` if table quality is sufficient.
     */
    fun validate(table: List<List<String>>): QualityResult {
        // Skip header row
        val dataRows = table.drop(1)
        if (dataRows.size < MIN_ROWS_FOR_CHECK) return QualityResult(isAcceptable = true)

        // Check A: Fragment detection — text cells starting with lowercase
        fragmentCheck(dataRows)?.let { return it }

        // Check B: Short cell prevalence
        shortCellCheck(dataRows)?.let { return it }

        // Check C: Date parse rate in first column
        dateParseCheck(dataRows)?.let { return it }

        return QualityResult(isAcceptable = true)
    }

    /**
     * Detects word fragments: cells starting with a lowercase letter indicate
     * that column boundaries split words (e.g. "полнение" from "Пополнение").
     */
    private fun fragmentCheck(dataRows: List<List<String>>): QualityResult? {
        // Collect text cells from columns 1+ (skip date/amount in col 0)
        val textCells = dataRows.flatMap { row ->
            row.drop(1).filter { it.isNotBlank() }
        }
        if (textCells.size < MIN_ROWS_FOR_CHECK) return null

        val fragmentCount = textCells.count { cell ->
            val trimmed = cell.trim()
            CYRILLIC_LOWERCASE_START.containsMatchIn(trimmed) ||
                LATIN_LOWERCASE_START.containsMatchIn(trimmed)
        }
        val ratio = fragmentCount.toFloat() / textCells.size
        if (ratio > FRAGMENT_THRESHOLD) {
            val reason = "Fragment detection: %.0f%% of text cells start with lowercase (threshold %.0f%%)".format(
                ratio * 100, FRAGMENT_THRESHOLD * 100,
            )
            Timber.d("TableQualityValidator: %s", reason)
            return QualityResult(isAcceptable = false, reason = reason)
        }
        return null
    }

    /**
     * Detects over-split columns: too many cells with ≤ 2 characters
     * (e.g. "По", "нк", "в" from split words).
     */
    private fun shortCellCheck(dataRows: List<List<String>>): QualityResult? {
        val textCells = dataRows.flatMap { row ->
            row.drop(1).filter { it.isNotBlank() }
        }
        if (textCells.size < MIN_ROWS_FOR_CHECK) return null

        val shortCount = textCells.count { it.trim().length <= 2 }
        val ratio = shortCount.toFloat() / textCells.size
        if (ratio > SHORT_CELL_THRESHOLD) {
            val reason = "Short cell prevalence: %.0f%% of text cells are ≤2 chars (threshold %.0f%%)".format(
                ratio * 100, SHORT_CELL_THRESHOLD * 100,
            )
            Timber.d("TableQualityValidator: %s", reason)
            return QualityResult(isAcceptable = false, reason = reason)
        }
        return null
    }

    /**
     * Checks that the first column contains dates. If boundaries are shifted,
     * the date column will contain non-date content.
     */
    private fun dateParseCheck(dataRows: List<List<String>>): QualityResult? {
        val firstColumnCells = dataRows.mapNotNull { row ->
            row.firstOrNull()?.trim()?.takeIf { it.isNotBlank() }
        }
        if (firstColumnCells.size < MIN_ROWS_FOR_CHECK) return null

        val dateCount = firstColumnCells.count { DATE_PATTERN.containsMatchIn(it) }
        val ratio = dateCount.toFloat() / firstColumnCells.size
        if (ratio < DATE_PARSE_THRESHOLD) {
            val reason = "Date parse rate: %.0f%% of first-column cells contain dates (threshold %.0f%%)".format(
                ratio * 100, DATE_PARSE_THRESHOLD * 100,
            )
            Timber.d("TableQualityValidator: %s", reason)
            return QualityResult(isAcceptable = false, reason = reason)
        }
        return null
    }
}
