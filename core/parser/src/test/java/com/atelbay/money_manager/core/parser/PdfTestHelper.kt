package com.atelbay.money_manager.core.parser

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper

/**
 * JVM test utility for extracting text from PDF test fixtures.
 *
 * Does NOT call PDFBoxResourceLoader.init(context) — not needed for
 * standard Latin-encoded bank statements. If a bank's PDF uses non-Latin
 * characters requiring CMaps, extraction may return incomplete text;
 * in that case the test assertion will fail with a clear message.
 */
object PdfTestHelper {
    fun extractText(resourceName: String): String {
        val stream = PdfTestHelper::class.java.classLoader!!
            .getResourceAsStream(resourceName)
            ?: error("Test resource not found: $resourceName. Copy PDF to core/parser/src/test/resources/")
        val bytes = stream.readBytes()
        val doc = PDDocument.load(bytes)
        return PDFTextStripper().apply { sortByPosition = true }.getText(doc)
            .also { doc.close() }
    }

    fun loadResource(resourceName: String): String {
        return PdfTestHelper::class.java.classLoader!!
            .getResourceAsStream(resourceName)!!.bufferedReader().readText()
    }
}
