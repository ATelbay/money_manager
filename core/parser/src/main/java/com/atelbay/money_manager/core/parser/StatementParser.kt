package com.atelbay.money_manager.core.parser

import com.atelbay.money_manager.core.model.ParsedTransaction
import com.atelbay.money_manager.core.remoteconfig.ParserConfigProvider
import timber.log.Timber
import javax.inject.Inject

data class RegexParseResult(
    val transactions: List<ParsedTransaction>,
    val bankId: String?,
)

class StatementParser @Inject constructor(
    private val pdfTextExtractor: PdfTextExtractor,
    private val bankDetector: BankDetector,
    private val regexParser: RegexStatementParser,
    private val configProvider: ParserConfigProvider,
) {

    suspend fun tryParsePdf(bytes: ByteArray): RegexParseResult? {
        val text = pdfTextExtractor.extract(bytes)
        if (text.isBlank()) {
            Timber.d("PDF text extraction returned empty result")
            return null
        }

        val configs = configProvider.getConfigs()
        val config = bankDetector.detect(text, configs)
        if (config == null) {
            Timber.d("No bank detected in PDF text")
            return null
        }

        Timber.d("Detected bank: %s", config.bankId)
        val transactions = regexParser.parse(text, config)

        if (transactions.isEmpty()) {
            Timber.d("RegEx parser returned 0 transactions for bank %s", config.bankId)
            return null
        }

        return RegexParseResult(transactions = transactions, bankId = config.bankId)
    }
}
