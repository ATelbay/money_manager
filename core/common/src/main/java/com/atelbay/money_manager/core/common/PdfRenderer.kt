package com.atelbay.money_manager.core.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

suspend fun renderPdfToImages(uri: Uri, context: Context): List<ByteArray> =
    withContext(Dispatchers.IO) {
        val pages = mutableListOf<ByteArray>()
        context.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
            val renderer = PdfRenderer(fd)
            try {
                for (i in 0 until renderer.pageCount) {
                    renderer.openPage(i).use { page ->
                        val bitmap = Bitmap.createBitmap(
                            page.width * 2,
                            page.height * 2,
                            Bitmap.Config.ARGB_8888,
                        )
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val stream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                        pages.add(stream.toByteArray())
                        bitmap.recycle()
                    }
                }
            } finally {
                renderer.close()
            }
        }
        pages
    }
