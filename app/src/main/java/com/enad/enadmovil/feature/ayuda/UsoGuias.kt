package com.enad.enadmovil.feature.ayuda

import android.content.Context

/** Lo que ve el docente en Ayuda: cuántas guías inició, cuántas terminó y cuál usa más. */
data class ResumenUso(val iniciadas: Int, val completadas: Int, val masUsadas: List<Guia>)

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

    fun leer(context: Context, uid: String): ResumenUso {
        val prefs = context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
        val conteos = Guia.entries.associateWith { guia ->
            prefs.getInt(clave(uid, guia, INICIADAS), 0) to prefs.getInt(clave(uid, guia, COMPLETADAS), 0)
        }
        return resumir(conteos)
    }

    /**
     * [conteos]: por guía, (iniciadas, completadas). Las más usadas son las de más inicios,
     * y si dos empatan salen las dos, igual que en Flutter.
     */
    fun resumir(conteos: Map<Guia, Pair<Int, Int>>): ResumenUso {
        val maximo = conteos.values.maxOfOrNull { it.first } ?: 0
        return ResumenUso(
            iniciadas = conteos.values.sumOf { it.first },
            // Una guía no puede terminarse más veces de las que se empezó
            completadas = conteos.values.sumOf { minOf(it.second, it.first) },
            masUsadas = if (maximo == 0) emptyList() else Guia.entries.filter { conteos[it]?.first == maximo }
        )
    }

    private fun sumar(context: Context, clave: String) {
        val prefs = context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
        prefs.edit().putInt(clave, prefs.getInt(clave, 0) + 1).apply()
    }
}
