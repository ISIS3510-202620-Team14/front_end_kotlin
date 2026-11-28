package com.enad.enadmovil.data.cache

import android.content.Context
import android.util.Log
import java.io.File

/**
 * CACHING (Sprint 4): archivos en la carpeta de caché de la app, caché y si no red, con política
 * de vencimiento y un máximo de archivos. La respuesta de cada institución vale 30 min; los
 * archivos que no se actualizan hace 7 días se borran y quedan máximo 60. Es temporal a propósito:
 * la copia persistente está en SQLite (NivelesDatabase).
 */
class CacheArchivosNiveles(
    private val carpeta: File,
    private val reloj: () -> Long = System::currentTimeMillis,
    private val registro: (String) -> Unit = { Log.d(TAG, it) }
) {
    constructor(context: Context) : this(File(context.applicationContext.cacheDir, CARPETA))

    var desdeCache = 0
        private set
    var desdeRed = 0
        private set

    /** El JSON guardado mientras siga vigente; null = hay que ir a la red. */
    fun fresco(clave: String): String? {
        val archivo = archivo(clave)
        val edad = reloj() - archivo.lastModified()
        if (!archivo.isFile || edad >= VIGENCIA_MS) {
            desdeRed++
            registro("vencido o sin archivo: $clave · caché=$desdeCache red=$desdeRed")
            return null
        }
        val json = runCatching { archivo.readText(Charsets.UTF_8) }.getOrNull()
        if (json == null) {
            desdeRed++
            return null
        }
        desdeCache++
        registro("desde caché: $clave (${(VIGENCIA_MS - edad) / 60_000} min más) · caché=$desdeCache red=$desdeRed")
        return json
    }

    /** Copia una respuesta de la red a la caché, vigente por 30 min, y limpia lo viejo. */
    fun guardar(clave: String, json: String) {
        carpeta.mkdirs()
        val destino = archivo(clave)
        val temporal = File(carpeta, "${destino.name}.tmp")
        temporal.writeText(json, Charsets.UTF_8)
        if (!temporal.renameTo(destino)) {
            destino.delete()
            if (!temporal.renameTo(destino)) temporal.delete()
        }
        destino.setLastModified(reloj())
        limpiar()
    }

    /** Borra los archivos sin actualizar en 7 días y deja como máximo 60, los más recientes. */
    internal fun limpiar() {
        val archivos = carpeta.listFiles { f -> f.isFile && f.name.endsWith(".json") } ?: return
        val ahora = reloj()
        val vigentes = archivos.filter { f ->
            val viejo = ahora - f.lastModified() >= SIN_USO_MS
            if (viejo) f.delete()
            !viejo
        }
        vigentes.sortedByDescending { it.lastModified() }.drop(MAX_ARCHIVOS).forEach { it.delete() }
    }

    private fun archivo(clave: String) = File(carpeta, clave.replace(Regex("[^A-Za-z0-9_-]"), "_") + ".json")

    companion object {
        const val CARPETA = "enad_niveles_cache"
        const val VIGENCIA_MS = 30 * 60 * 1000L
        const val SIN_USO_MS = 7 * 24 * 60 * 60 * 1000L
        const val MAX_ARCHIVOS = 60
        private const val TAG = "niveles-cache"
    }
}
