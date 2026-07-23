package com.example.cashmanage.util

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

class OcrManager(private val context: Context) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractTextFromImage(uri: Uri): String {
        return try {
            val image = InputImage.fromFilePath(context, uri)
            val result = recognizer.process(image).await()
            result.text
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    fun parseAmount(text: String): Double? {
        // Regex to find things that look like prices/totals
        val regex = Regex("""(total|rp|rp\.|amount)?\s*[\d,.]+""", RegexOption.IGNORE_CASE)
        val match = regex.find(text)
        return match?.value?.replace(Regex("""[^\d.]"""), "")?.toDoubleOrNull()
    }
}
