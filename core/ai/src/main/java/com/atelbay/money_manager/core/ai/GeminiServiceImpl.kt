package com.atelbay.money_manager.core.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiServiceImpl @Inject constructor() : GeminiService {

    private val generativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(
                modelName = "gemini-2.5-flash",
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                },
            )
    }

    override suspend fun parseContent(
        blobs: List<Pair<ByteArray, String>>,
        prompt: String,
    ): String {
        Timber.d(">>> Gemini request: %d blob(s), prompt length=%d", blobs.size, prompt.length)
        Timber.d(">>> Gemini prompt:\n%s", prompt)

        val inputContent = content {
            blobs.forEach { (bytes, mimeType) ->
                inlineData(bytes, mimeType)
            }
            text(prompt)
        }

        return try {
            val response = generativeModel.generateContent(inputContent)
            val text = response.text.orEmpty()
            Timber.d("<<< Gemini response (length=%d):\n%s", text.length, text)
            text
        } catch (e: Exception) {
            Timber.e(e, "<<< Gemini API call failed")
            throw e
        }
    }
}
