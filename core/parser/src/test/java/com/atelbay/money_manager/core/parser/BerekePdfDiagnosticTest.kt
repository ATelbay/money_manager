package com.atelbay.money_manager.core.parser

import org.junit.Assert.assertTrue
import org.junit.Test

class BerekePdfDiagnosticTest {

    @Test
    fun `print raw PdfBox output for bereke_statement`() {
        val text = PdfTestHelper.extractText("bereke_statement.pdf")
        println("=== RAW PDFBOX OUTPUT ===")
        println(text)
        println("=== END ===")
        assertTrue("PDF text must not be blank", text.isNotBlank())
    }
}
