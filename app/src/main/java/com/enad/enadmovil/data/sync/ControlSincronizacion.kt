package com.enad.enadmovil.data.sync

import android.content.Context
import kotlinx.coroutines.sync.Semaphore

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

    private fun clave(estudianteId: String, fecha: String) = "$estudianteId|$fecha"

    /** Estado del servidor cuando el docente marcó por primera vez (no se pisa con marcas siguientes). */
    fun recordarBase(context: Context, estudianteId: String, fecha: String, estado: String) {
        val prefs = context.getSharedPreferences(BASES, Context.MODE_PRIVATE)
        if (!prefs.contains(clave(estudianteId, fecha))) prefs.edit().putString(clave(estudianteId, fecha), estado).apply()
    }

    fun base(context: Context, estudianteId: String, fecha: String): String? =
        context.getSharedPreferences(BASES, Context.MODE_PRIVATE).getString(clave(estudianteId, fecha), null)

    fun olvidarBase(context: Context, estudianteId: String, fecha: String) {
        context.getSharedPreferences(BASES, Context.MODE_PRIVATE).edit().remove(clave(estudianteId, fecha)).apply()
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
}
