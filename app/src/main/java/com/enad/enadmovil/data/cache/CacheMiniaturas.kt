package com.enad.enadmovil.data.cache

import android.content.ComponentCallbacks2
import android.graphics.Bitmap
import android.util.Log
import android.util.LruCache

/**
 * Miniaturas de las páginas escaneadas guardadas en memoria.
 *
 * El límite no es de cantidad sino de bytes: 1/8 de la memoria que Android le da a la app.
 * Cuando se llena, sale la miniatura que lleva más tiempo sin usarse (LRU).
 * No vencen por tiempo porque la foto de una página no cambia.
 */
object CacheMiniaturas {
    // LruCache mide en la unidad que devuelve sizeOf, aquí kilobytes
    private val maxKb = (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()

    private val lru = object : LruCache<String, Bitmap>(maxKb) {
        override fun sizeOf(key: String, value: Bitmap) = (value.byteCount / 1024).coerceAtLeast(1)
    }

    /**
     * El conteo sale en Logcat con el filtro "CacheMiniaturas". Con dos páginas ya escaneadas,
     * abrir otra vez la cámara da dos aciertos. Cada foto nueva da un solo fallo, el de la primera vez.
     */
    fun obtener(clave: String): Bitmap? {
        val miniatura = lru.get(clave)
        Log.d(TAG, "${if (miniatura != null) "ACIERTO" else "FALLO"} ${clave.substringAfterLast('/')} " +
            "(aciertos ${lru.hitCount()}, fallos ${lru.missCount()}, ${lru.size()} de $maxKb KB)")
        return miniatura
    }

    fun guardar(clave: String, miniatura: Bitmap) {
        lru.put(clave, miniatura)
    }

    fun limpiar() {
        lru.evictAll()
    }

    /**
     * Android avisa cuando le falta memoria. Si la app pasó a segundo plano se suelta todo,
     * y si solo se ocultó la pantalla se queda con la mitad de las miniaturas más recientes.
     */
    fun recortar(nivel: Int) {
        when {
            nivel >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> lru.evictAll()
            nivel >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> lru.trimToSize(lru.maxSize() / 2)
        }
    }

    private const val TAG = "CacheMiniaturas"
}
