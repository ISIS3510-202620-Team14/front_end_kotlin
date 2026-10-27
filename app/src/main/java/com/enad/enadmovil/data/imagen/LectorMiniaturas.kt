package com.enad.enadmovil.data.imagen

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

/**
 * Abre la foto de una página ya achicada. Una foto de la cámara abierta completa ocupa
 * decenas de MB, así que nunca se carga entera: primero se leen sus medidas y luego se
 * decodifica saltando píxeles (inSampleSize).
 */
object LectorMiniaturas {
    const val LADO_MAX = 200

    /** Devuelve la foto con su lado más largo en [ladoMax] píxeles, o null si no se pudo abrir. */
    fun leer(uri: Uri, ladoMax: Int = LADO_MAX): Bitmap? {
        val ruta = uri.path ?: return null
        // Solo las medidas, sin cargar la foto en memoria
        val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(ruta, medidas)
        if (medidas.outWidth <= 0 || medidas.outHeight <= 0) return null

        val opciones = BitmapFactory.Options().apply {
            inSampleSize = calcularMuestra(medidas.outWidth, medidas.outHeight, ladoMax)
        }
        val reducida = BitmapFactory.decodeFile(ruta, opciones) ?: return null
        return ajustar(reducida, ladoMax)
    }

    /**
     * inSampleSize solo acepta potencias de 2. Se usa la más grande que deja el lado largo
     * en al menos [ladoMax], y el ajuste final lo hace [ajustar].
     */
    fun calcularMuestra(ancho: Int, alto: Int, ladoMax: Int): Int {
        var muestra = 1
        while (maxOf(ancho, alto) / (muestra * 2) >= ladoMax) muestra *= 2
        return muestra
    }

    private fun ajustar(bitmap: Bitmap, ladoMax: Int): Bitmap {
        val lado = maxOf(bitmap.width, bitmap.height)
        if (lado <= ladoMax) return bitmap
        val escala = ladoMax.toFloat() / lado
        val ajustada = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * escala).toInt().coerceAtLeast(1),
            (bitmap.height * escala).toInt().coerceAtLeast(1),
            true
        )
        if (ajustada !== bitmap) bitmap.recycle()
        return ajustada
    }
}
