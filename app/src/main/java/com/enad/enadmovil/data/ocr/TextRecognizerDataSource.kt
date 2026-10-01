package com.enad.enadmovil.data.ocr

import android.content.Context
import android.net.Uri
import com.enad.enadmovil.domain.model.LineaOcr
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/** OCR con el modelo embebido de ML Kit: corre en el teléfono, sin internet. */
class TextRecognizerDataSource(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun reconocer(uri: Uri): List<LineaOcr> {
        val imagen = InputImage.fromFilePath(context, uri)
        val resultado = recognizer.process(imagen).await()
        return resultado.textBlocks.flatMap { it.lines }.mapNotNull { linea ->
            linea.boundingBox?.let { LineaOcr(linea.text, it.left, it.top, it.bottom) }
        }
    }
}