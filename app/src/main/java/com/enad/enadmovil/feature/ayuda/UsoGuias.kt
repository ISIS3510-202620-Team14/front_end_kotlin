package com.enad.enadmovil.feature.ayuda

import android.content.Context

/**
 * Cuántas veces el docente empezó y terminó cada guía en este celular (BQ 9, tipo 2).
 * Se guarda por cuenta en SharedPreferences, así funciona sin internet y otro docente
 * que entre en el mismo celular no ve el uso del anterior.
 */
object UsoGuias {
    private const val ARCHIVO = "enad_uso_guias"
    private const val INICIADAS = "iniciadas"
    private const val COMPLETADAS = "completadas"

    private fun clave(uid: String, guia: Guia, campo: String) = "$uid|${guia.name}|$campo"

    fun registrarInicio(context: Context, uid: String, guia: Guia) = sumar(context, clave(uid, guia, INICIADAS))

    fun registrarFin(context: Context, uid: String, guia: Guia) = sumar(context, clave(uid, guia, COMPLETADAS))

    private fun sumar(context: Context, clave: String) {
        val prefs = context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
        prefs.edit().putInt(clave, prefs.getInt(clave, 0) + 1).apply()
    }
}
