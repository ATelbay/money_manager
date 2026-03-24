package com.atelbay.money_manager.core.parser

/**
 * Shared regex fragments used across PDF-parsing components.
 *
 * Having a single source of truth prevents the date-detection patterns in
 * [PdfTableExtractor] and [StatementParser] from drifting apart silently.
 */
internal object ParserPatterns {
    /**
     * Core date fragment: matches the start of a date token (DD.MM, YYYY-MM, etc.).
     * Does NOT include a leading anchor or trailing year-suffix — callers compose
     * the full pattern from this fragment according to their own needs.
     *
     * Examples matched: "01.03", "2024-12", "31/07"
     */
    const val DATE_CORE = "\\d{2,4}[-./]\\d{2}"
}
