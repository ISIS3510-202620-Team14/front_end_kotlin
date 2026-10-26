package com.enad.enadmovil.data.sync

import android.content.Context
import kotlinx.coroutines.sync.Semaphore
import org.json.JSONArray
import org.json.JSONObject

/**
 * Semáforos de la sincronización de asistencia y lo que se recuerda entre una subida y otra.
 *
 * - [turno] (1 permiso): solo una sincronización a la vez en este celular. Si el SyncWorker y el
 *   botón Guardar llegan juntos, el que no consigue el permiso no espera: se le avisa y reintenta luego.
 * - [envios] (4 permisos): como máximo 4 PUT de asistencia en vuelo al mismo tiempo.
 *
 * Entre dos celulares no hay semáforo compartido (el backend no tiene un candado), así que el
 * recurso compartido, la marca de un estudiante en un día, se protege comparando versiones:
 * se guarda el estado que tenía el servidor cuando el docente marcó ([recordarBase]); si al subir
 * el servidor ya tiene otro valor, otro celular llegó primero y esta marca no se envía.
 */
object ControlSincronizacion {
    val turno = Semaphore(permits = 1)
    val envios = Semaphore(permits = 4)

    private const val BASES = "enad_asistencia_base"
    private const val AVISOS = "enad_asistencia_avisos"
    private const val AVISO = "aviso"
    private const val REEMPLAZADAS = "enad_asistencia_reemplazadas"
    private const val LISTA = "lista"
    private const val MAX_REEMPLAZADAS = 50

    private fun clave(estudianteId: String, fecha: String) = "$estudianteId|$fecha"
    private fun claveRed(estudianteId: String, fecha: String) = "${clave(estudianteId, fecha)}|red"

    /**
     * Estado del servidor cuando el docente marcó por primera vez (no se pisa con marcas siguientes),
     * junto con la conectividad que tenía el celular en ese momento ("online" / "offline").
     */
    fun recordarBase(context: Context, estudianteId: String, fecha: String, estado: String, conectividad: String) {
        val prefs = context.getSharedPreferences(BASES, Context.MODE_PRIVATE)
        if (!prefs.contains(clave(estudianteId, fecha))) {
            prefs.edit()
                .putString(clave(estudianteId, fecha), estado)
                .putString(claveRed(estudianteId, fecha), conectividad)
                .apply()
        }
    }

    fun conectividadAlMarcar(context: Context, estudianteId: String, fecha: String): String? =
        context.getSharedPreferences(BASES, Context.MODE_PRIVATE).getString(claveRed(estudianteId, fecha), null)

    fun base(context: Context, estudianteId: String, fecha: String): String? =
        context.getSharedPreferences(BASES, Context.MODE_PRIVATE).getString(clave(estudianteId, fecha), null)

    fun olvidarBase(context: Context, estudianteId: String, fecha: String) {
        context.getSharedPreferences(BASES, Context.MODE_PRIVATE).edit()
            .remove(clave(estudianteId, fecha))
            .remove(claveRed(estudianteId, fecha))
            .apply()
    }

    /** Aviso para el docente cuando la sincronización corrió en segundo plano (lo muestra Asistencia). */
    fun guardarAviso(context: Context, texto: String) {
        context.getSharedPreferences(AVISOS, Context.MODE_PRIVATE).edit().putString(AVISO, texto).apply()
    }

    fun tomarAviso(context: Context): String? {
        val prefs = context.getSharedPreferences(AVISOS, Context.MODE_PRIVATE)
        val texto = prefs.getString(AVISO, null) ?: return null
        prefs.edit().remove(AVISO).apply()
        return texto
    }

    /** Marca del docente que no se aplicó porque otro celular ya había guardado otro valor ese día. */
    data class MarcaReemplazada(
        val estudianteId: String,
        val nombre: String,
        val fecha: String,
        val enviada: String,   // EstadoRemoto que marcó este docente
        val conservada: String // EstadoRemoto que quedó en el servidor
    )

    /** Guarda las marcas reemplazadas para que el docente las revise en Asistencia. */
    fun guardarReemplazadas(context: Context, nuevas: List<MarcaReemplazada>) {
        if (nuevas.isEmpty()) return
        val claves = nuevas.map { clave(it.estudianteId, it.fecha) }.toSet()
        val lista = (reemplazadas(context).filter { clave(it.estudianteId, it.fecha) !in claves } + nuevas)
            .takeLast(MAX_REEMPLAZADAS)
        escribirReemplazadas(context, lista)
    }

    fun reemplazadas(context: Context): List<MarcaReemplazada> {
        val texto = context.getSharedPreferences(REEMPLAZADAS, Context.MODE_PRIVATE).getString(LISTA, null)
            ?: return emptyList()
        return runCatching {
            val arreglo = JSONArray(texto)
            List(arreglo.length()) { i ->
                val o = arreglo.getJSONObject(i)
                MarcaReemplazada(
                    estudianteId = o.getString("estudianteId"),
                    nombre = o.optString("nombre"),
                    fecha = o.getString("fecha"),
                    enviada = o.getString("enviada"),
                    conservada = o.getString("conservada")
                )
            }
        }.getOrDefault(emptyList())
    }

    /** El docente ya la revisó: sale de la lista. */
    fun descartarReemplazada(context: Context, estudianteId: String, fecha: String) {
        escribirReemplazadas(context, reemplazadas(context).filterNot { it.estudianteId == estudianteId && it.fecha == fecha })
    }

    private fun escribirReemplazadas(context: Context, lista: List<MarcaReemplazada>) {
        val arreglo = JSONArray()
        lista.forEach {
            arreglo.put(
                JSONObject()
                    .put("estudianteId", it.estudianteId)
                    .put("nombre", it.nombre)
                    .put("fecha", it.fecha)
                    .put("enviada", it.enviada)
                    .put("conservada", it.conservada)
            )
        }
        context.getSharedPreferences(REEMPLAZADAS, Context.MODE_PRIVATE).edit().putString(LISTA, arreglo.toString()).apply()
    }
}
