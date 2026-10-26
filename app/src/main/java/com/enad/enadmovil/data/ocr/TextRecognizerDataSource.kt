package com.enad.enadmovil.data.ocr

import android.content.Context
import android.net.Uri
import com.enad.enadmovil.domain.model.LineaOcr
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor

/**
 * OCR con el modelo embebido de ML Kit: corre en el teléfono, sin internet.
 * Si llega un [executor], ML Kit hace el reconocimiento en esos hilos y no en los suyos.
 */
class TextRecognizerDataSource(private val context: Context, executor: Executor? = null) {

    private val recognizer = TextRecognition.getClient(
        executor?.let { TextRecognizerOptions.Builder().setExecutor(it).build() }
            ?: TextRecognizerOptions.DEFAULT_OPTIONS
    )

    suspend fun reconocer(uri: Uri): List<LineaOcr> {
        // Abrir la foto la lee del disco y la decodifica completa: va en el hilo de disco, nunca en el principal
        val imagen = withContext(Dispatchers.IO) { InputImage.fromFilePath(context, uri) }
        val resultado = recognizer.process(imagen).await()
        return resultado.textBlocks.flatMap { it.lines }.mapNotNull { linea ->
            linea.boundingBox?.let { LineaOcr(linea.text, it.left, it.top, it.bottom) }
        }
    }

    /** Libera el modelo de ML Kit. */
    fun cerrar() = recognizer.close()
}
