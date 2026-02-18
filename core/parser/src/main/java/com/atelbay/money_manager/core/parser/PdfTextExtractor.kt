package com.atelbay.money_manager.core.parser

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.ByteArrayInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfTextExtractor @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private var initialized = false

    private fun ensureInitialized() {
        if (!initialized) {
            PDFBoxResourceLoader.init(context)
            initialized = true
        }
    }

    fun extract(bytes: ByteArray): String {
        return try {
            ensureInitialized()
            ByteArrayInputStream(bytes).use { stream ->
                PDDocument.load(stream).use { document ->
                    val stripper = PDFTextStripper().apply {
                        sortByPosition = true
                    }
                    stripper.getText(document)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to extract text from PDF (%d bytes)", bytes.size)
            ""
        }
    }
}
