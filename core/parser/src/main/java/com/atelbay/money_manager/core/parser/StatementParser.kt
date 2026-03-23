package com.atelbay.money_manager.core.parser

import com.atelbay.money_manager.core.remoteconfig.ParserConfig
import com.atelbay.money_manager.core.model.ParsedTransaction
import com.atelbay.money_manager.core.model.TableParserConfig
import com.atelbay.money_manager.core.remoteconfig.ParserConfigProvider
import timber.log.Timber
import javax.inject.Inject

data class RegexParseResult(
    val transactions: List<ParsedTransaction>,
    val bankId: String?,
    val extractedText: String = "",
)

data class TableParseResult(
    val transactions: List<ParsedTransaction>,
    val bankId: String?,
    val extractedTable: List<List<String>> = emptyList(),
)

data class TableExtractionResult(
    val sampleRows: List<List<String>>,
    val metadataRows: List<List<String>>,
    val columnHeaderRow: List<String>?,
)

class StatementParser @Inject constructor(
    private val pdfTextExtractor: PdfTextExtractor,
    private val bankDetector: BankDetector,
    private val regexParser: RegexStatementParser,
    private val configProvider: ParserConfigProvider,
    private val pdfTableExtractor: PdfTableExtractor,
    private val tableStatementParser: TableStatementParser,
) {

    private companion object {
        const val HEADER_LINE_COUNT = 10
        const val SAMPLE_LINE_COUNT = 60
    }

    suspend fun tryParsePdf(
        bytes: ByteArray,
        additionalConfigs: List<ParserConfig> = emptyList(),
    ): RegexParseResult? {
        val text = pdfTextExtractor.extract(bytes)
        if (text.isBlank()) {
            Timber.d("PDF text extraction returned empty result")
            return RegexParseResult(transactions = emptyList(), bankId = null, extractedText = text)
        }

        val configs = configProvider.getConfigs() + additionalConfigs
        val matchingConfigs = bankDetector.detectAll(text, configs)
        if (matchingConfigs.isEmpty()) {
            Timber.d("No bank detected in PDF text")
            return RegexParseResult(transactions = emptyList(), bankId = null, extractedText = text)
        }

        for (config in matchingConfigs) {
            Timber.d("Trying config for bank: %s", config.bankId)
            val transactions = regexParser.parse(text, config)
            if (transactions.isNotEmpty()) {
                Timber.d("Parsed %d transactions with config for bank %s", transactions.size, config.bankId)
                return RegexParseResult(transactions = transactions, bankId = config.bankId, extractedText = text)
            }
        }

        val firstBankId = matchingConfigs.first().bankId
        Timber.d("All %d configs tried, 0 transactions for bank %s", matchingConfigs.size, firstBankId)
        return RegexParseResult(transactions = emptyList(), bankId = firstBankId, extractedText = text)
    }

    fun tryParseWithConfig(bytes: ByteArray, config: ParserConfig): RegexParseResult {
        val text = pdfTextExtractor.extract(bytes)
        val transactions = regexParser.parse(text, config)
        return RegexParseResult(transactions = transactions, bankId = config.bankId)
    }

    fun extractHeaderSnippet(text: String): String =
        text.lines()
            .take(HEADER_LINE_COUNT)
            .filter { it.isNotBlank() }
            .joinToString("\n")

    fun extractSampleRows(text: String): String {
        return text.lines()
            .drop(HEADER_LINE_COUNT)
            .filter { it.isNotBlank() }
            .take(SAMPLE_LINE_COUNT)
            .joinToString("\n")
    }

    fun tryParseTable(bytes: ByteArray, tableConfigs: List<TableParserConfig>): TableParseResult? {
        val table = pdfTableExtractor.extractTableOrNull(bytes) ?: return null
        val allCellText = table.flatten().joinToString(" ")
        for (config in tableConfigs) {
            val matches = config.bankMarkers.any { marker -> allCellText.contains(marker, ignoreCase = true) }
            if (matches) {
                Timber.d("Table bank marker matched for bankId: %s", config.bankId)
                val transactions = tableStatementParser.parse(table, config)
                if (transactions.isNotEmpty()) {
                    Timber.d("Parsed %d transactions with table config for bank %s", transactions.size, config.bankId)
                    return TableParseResult(transactions = transactions, bankId = config.bankId, extractedTable = table)
                }
            }
        }
        return null
    }

    fun tryParseWithTableConfig(bytes: ByteArray, config: TableParserConfig): TableParseResult {
        val table = pdfTableExtractor.extractTable(bytes)
        val transactions = tableStatementParser.parse(table, config)
        return TableParseResult(transactions = transactions, bankId = config.bankId, extractedTable = table)
    }

    fun extractSampleTableRows(bytes: ByteArray): List<List<String>> {
        return extractSampleTableRowsWithContext(bytes).sampleRows
    }

    fun extractSampleTableRowsWithContext(bytes: ByteArray): TableExtractionResult {
        val table = pdfTableExtractor.extractTable(bytes)
        if (table.isEmpty()) return TableExtractionResult(emptyList(), emptyList(), null)

        // Determine dominant column structure: most common non-empty cell count.
        // Metadata rows (e.g. "Branch:Headbank") have 1-2 non-empty cells
        // while transaction rows have 5-7. Filter by modal count.
        val nonEmptyCounts = table.map { row -> row.count { it.isNotBlank() } }
        val modalCount = nonEmptyCounts
            .groupBy { it }
            .maxByOrNull { it.value.size }
            ?.key ?: 1
        val threshold = (modalCount / 2).coerceAtLeast(2)

        val metadataRows = table.filter { row ->
            row.count { it.isNotBlank() } < threshold
        }
        val structuralRows = table.filter { row ->
            row.count { it.isNotBlank() } >= threshold
        }

        return TableExtractionResult(
            sampleRows = structuralRows.drop(1).take(10),
            metadataRows = metadataRows,
            columnHeaderRow = structuralRows.firstOrNull(),
        )
    }
}
