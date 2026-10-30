package com.enad.enadmovil.data.local

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Una evaluación hecha sin conexión que todavía no llega al backend. */
data class EvaluacionPendiente(
    val clientId: String,       // el backend no la duplica si llega dos veces con el mismo id
    val estudianteId: String,
    val materia: String,        // clave del backend: lectura | matematicas
    val nivel: String,          // clave del backend (ej. un_digito)
    val etiqueta: String,       // lo que ve el docente (ej. "1 dígito")
    val tipo: String,           // momento: inicial | media | final | especifica
    val fecha: String,          // ISO, el momento en que el docente evaluó
    val sexo: String,
    val edad: Int,
    val muestra: Int
)

/**
 * Cola de evaluaciones hechas sin conexión, guardada en el celular por docente. La escribe la
 * pantalla de Clasificación y la vacía EnviarEvaluacionesWorker a medida que el backend confirma.
 * Los métodos son synchronized porque la pantalla y el worker la tocan al mismo tiempo.
 */
object EvaluacionesPendientes {
    private const val PREFS = "enad_evaluaciones_pendientes"

    private fun clave(uid: String) = "pendientes_$uid"

    @Synchronized
    fun agregar(context: Context, uid: String, evaluacion: EvaluacionPendiente) {
        escribir(context, uid, todas(context, uid).filterNot { it.clientId == evaluacion.clientId } + evaluacion)
    }

    @Synchronized
    fun quitar(context: Context, uid: String, clientId: String) {
        escribir(context, uid, todas(context, uid).filterNot { it.clientId == clientId })
    }

    @Synchronized
    fun todas(context: Context, uid: String): List<EvaluacionPendiente> {
        val texto = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(clave(uid), null)
            ?: return emptyList()
        return runCatching {
            val arreglo = JSONArray(texto)
            List(arreglo.length()) { i ->
                val o = arreglo.getJSONObject(i)
                EvaluacionPendiente(
                    clientId = o.getString("clientId"),
                    estudianteId = o.getString("estudianteId"),
                    materia = o.getString("materia"),
                    nivel = o.getString("nivel"),
                    etiqueta = o.getString("etiqueta"),
                    tipo = o.getString("tipo"),
                    fecha = o.getString("fecha"),
                    sexo = o.getString("sexo"),
                    edad = o.getInt("edad"),
                    muestra = o.getInt("muestra")
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun escribir(context: Context, uid: String, lista: List<EvaluacionPendiente>) {
        val arreglo = JSONArray()
        lista.forEach {
            arreglo.put(
                JSONObject()
                    .put("clientId", it.clientId)
                    .put("estudianteId", it.estudianteId)
                    .put("materia", it.materia)
                    .put("nivel", it.nivel)
                    .put("etiqueta", it.etiqueta)
                    .put("tipo", it.tipo)
                    .put("fecha", it.fecha)
                    .put("sexo", it.sexo)
                    .put("edad", it.edad)
                    .put("muestra", it.muestra)
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(clave(uid), arreglo.toString()).apply()
    }
}
