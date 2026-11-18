package com.enad.enadmovil.data.cache

import android.content.ComponentCallbacks2
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/**
 * Miniaturas de las páginas escaneadas en dos niveles.
 *
 * 1. Memoria: el límite no es de cantidad sino de bytes, 1/8 de la memoria que Android le da a
 *    la app. Cuando se llena, sale la miniatura que lleva más tiempo sin usarse (LRU).
 * 2. Disco: la misma miniatura como un JPEG pequeño en el almacenamiento privado de la app.
 *    Sobrevive a cerrar la app, así al volver no hay que abrir otra vez la foto completa.
 *
 * No vencen por tiempo porque la foto de una página no cambia.
 */
object CacheMiniaturas {
    // LruCache mide en la unidad que devuelve sizeOf, aquí kilobytes
    private val maxKb = (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()

    private val lru = object : LruCache<String, Bitmap>(maxKb) {
        override fun sizeOf(key: String, value: Bitmap) = (value.byteCount / 1024).coerceAtLeast(1)
    }

    @Volatile private var carpeta: File? = null
    private val aciertosDisco = AtomicInteger()

    /** Carpeta del nivel en disco. Sin carpeta la caché solo usa memoria. */
    fun usarCarpeta(directorio: File) {
        carpeta = directorio
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

    /** Segundo nivel. Lee del disco, así que se llama desde un hilo de IO. Si está, vuelve a memoria. */
    fun leerDeDisco(clave: String): Bitmap? {
        val archivo = archivoDe(clave)?.takeIf { it.exists() } ?: return null
        val miniatura = BitmapFactory.decodeFile(archivo.path, null) ?: return null
        lru.put(clave, miniatura)
        Log.d(TAG, "ACIERTO DISCO ${archivo.name} (aciertos en disco ${aciertosDisco.incrementAndGet()})")
        return miniatura
    }

    /** Guarda en memoria y en disco. Escribe un archivo, así que se llama desde un hilo de IO. */
    fun guardar(clave: String, miniatura: Bitmap) {
        lru.put(clave, miniatura)
        val archivo = archivoDe(clave) ?: return
        runCatching {
            archivo.parentFile?.mkdirs()
            // Primero a un temporal y luego se renombra, así nunca queda una miniatura a medias
            val temporal = File(archivo.parentFile, "${archivo.name}.tmp")
            temporal.outputStream().use { miniatura.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            if (!temporal.renameTo(archivo)) temporal.delete()
        }
    }

    /** Vacía la memoria. El disco se borra aparte con [borrarDeDisco]. */
    fun limpiar() {
        lru.evictAll()
    }

    /** Borra del disco las miniaturas de un escaneo que ya terminó. Se llama desde un hilo de IO. */
    fun borrarDeDisco(claves: List<String>) {
        claves.forEach { archivoDe(it)?.delete() }
    }

    /**
     * Android avisa cuando le falta memoria. Si la app pasó a segundo plano se suelta todo,
     * y si solo se ocultó la pantalla se queda con la mitad de las miniaturas más recientes.
     * El disco no se toca: no ocupa memoria.
     */
    fun recortar(nivel: Int) {
        when {
            nivel >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> lru.evictAll()
            nivel >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> lru.trimToSize(lru.maxSize() / 2)
        }
    }

    // La clave es la Uri de la foto (file:///.../lista_<hora>.jpg), así que su nombre ya es único
    private fun archivoDe(clave: String): File? = carpeta?.let {
        File(it, "min_" + clave.substringAfterLast('/').replace(Regex("[^A-Za-z0-9._-]"), "_"))
    }

    private const val TAG = "CacheMiniaturas"
}
