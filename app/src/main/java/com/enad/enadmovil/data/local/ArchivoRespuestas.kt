package com.enad.enadmovil.data.local

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class Lectura<T>(val datos: T, val guardadoEn: Long? = null)

class ArchivoRespuestas(private val carpeta: File) {
    constructor(context: Context) : this(File(context.applicationContext.filesDir, CARPETA))

    data class Guardado(val json: String, val guardadoEn: Long)

    suspend fun guardar(uid: String, nombre: String, json: String) = withContext(Dispatchers.IO) {
        carpeta.mkdirs()
        val destino = File(carpeta, nombreArchivo(uid, nombre))
        val temporal = File(carpeta, "${destino.name}.tmp")
        temporal.writeText(json, Charsets.UTF_8)
        if (!temporal.renameTo(destino)) temporal.delete()
    }

    suspend fun leer(uid: String, nombre: String): Guardado? = withContext(Dispatchers.IO) {
        val archivo = File(carpeta, nombreArchivo(uid, nombre))
        runCatching { Guardado(archivo.readText(Charsets.UTF_8), archivo.lastModified()) }.getOrNull()
    }

    fun borrarTodo() {
        carpeta.deleteRecursively()
    }

    suspend fun <T> conRespaldo(
        uid: String?,
        nombre: String,
        esRechazo: (Exception) -> Boolean = { false },
        pedir: suspend () -> String,
        convertir: (String) -> T
    ): Lectura<T> {
        if (uid == null) return Lectura(convertir(pedir()))
        val json = try {
            pedir()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (esRechazo(e)) throw e
            val guardado = leer(uid, nombre) ?: throw e
            val datos = runCatching { convertir(guardado.json) }.getOrNull() ?: throw e
            return Lectura(datos, guardado.guardadoEn)
        }
        val datos = convertir(json)
        runCatching { guardar(uid, nombre, json) }
        return Lectura(datos)
    }

    companion object {
        const val CARPETA = "respuestas"

        internal fun nombreArchivo(uid: String, nombre: String): String =
            "${uid}_$nombre".replace(Regex("[^A-Za-z0-9_-]"), "_") + ".json"
    }
}