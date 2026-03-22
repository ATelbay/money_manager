package com.atelbay.money_manager.core.parser

import com.atelbay.money_manager.core.remoteconfig.ParserConfig
import com.atelbay.money_manager.core.model.ParsedTransaction
import com.atelbay.money_manager.core.remoteconfig.ParserConfigProvider
import timber.log.Timber
import javax.inject.Inject

data class RegexParseResult(
    val transactions: List<ParsedTransaction>,
    val bankId: String?,
    val extractedText: String = "",
)

class StatementParser @Inject constructor(
    private val pdfTextExtractor: PdfTextExtractor,
    private val bankDetector: BankDetector,
    private val regexParser: RegexStatementParser,
    private val configProvider: ParserConfigProvider,
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
}
