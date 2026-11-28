package com.enad.enadmovil.data.niveles

import android.util.JsonReader
import android.util.JsonToken
import com.enad.enadmovil.domain.model.NivelesInstitucion
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.StringReader

/*
 * MULTITHREADING (Sprint 4): decodificar el JSON de cientos de estudiantes y contarlos es trabajo de
 * CPU. Con una corrutina normal seguiría en el hilo principal y congelaría la pantalla, así que cada
 * institución se cuenta en Dispatchers.Default, el pool de hilos para trabajo de CPU, mientras la UI
 * sigue dibujando. La función solo recibe el texto del JSON y devuelve un IntArray nuevo, así que no
 * comparte estado con otros hilos.
 */

/**
 * Decodifica y cuenta una institución: los 42 conteos de NivelesInstitucion.
 *
 * MICRO-OPTIMIZACIÓN (Sprint 4): lee el JSON en streaming con JsonReader, en una sola pasada, y solo
 * toma grade, retired y levels.{materia}.orden. Antes, JSONObject armaba un objeto por cada campo de
 * cada estudiante (nombre, sede, edad…) y después se copiaba todo a otra lista para contarla.
 */
fun contarNiveles(estudiantesJson: String): IntArray {
    val conteos = IntArray(NivelesInstitucion.TAMANO)
    JsonReader(StringReader(estudiantesJson)).use { r ->
        if (r.peek() != JsonToken.BEGIN_OBJECT) return conteos
        r.beginObject()
        while (r.hasNext()) {
            if (r.nextName() == "students" && r.peek() == JsonToken.BEGIN_ARRAY) {
                r.beginArray()
                while (r.hasNext()) sumarDelJson(r, conteos)
                r.endArray()
            } else {
                r.skipValue()
            }
        }
        r.endObject()
    }
    return conteos
}

/** Lee un estudiante del arreglo y lo suma; los campos que no se usan se saltan sin crear objetos. */
private fun sumarDelJson(r: JsonReader, conteos: IntArray) {
    if (r.peek() != JsonToken.BEGIN_OBJECT) {
        r.skipValue()
        return
    }
    var grado = 0
    var retirado = false
    var lectura = 0
    var matematicas = 0
    r.beginObject()
    while (r.hasNext()) {
        when (r.nextName()) {
            "grade" -> grado = entero(r)
            "retired" -> retirado = if (r.peek() == JsonToken.BOOLEAN) r.nextBoolean() else { r.skipValue(); false }
            "levels" -> if (r.peek() == JsonToken.BEGIN_OBJECT) {
                r.beginObject()
                while (r.hasNext()) {
                    when (r.nextName()) {
                        "lectura" -> lectura = orden(r)
                        "matematicas" -> matematicas = orden(r)
                        else -> r.skipValue()
                    }
                }
                r.endObject()
            } else {
                r.skipValue()
            }
            else -> r.skipValue()
        }
    }
    r.endObject()
    sumarEstudiante(conteos, grado, retirado, lectura, matematicas)
}

/** levels.{materia}: null si todavía no tiene nivel, si no un objeto con orden (1..6). */
private fun orden(r: JsonReader): Int {
    if (r.peek() != JsonToken.BEGIN_OBJECT) {
        r.skipValue()
        return 0
    }
    var orden = 0
    r.beginObject()
    while (r.hasNext()) {
        if (r.nextName() == "orden") orden = entero(r) else r.skipValue()
    }
    r.endObject()
    return orden
}

private fun entero(r: JsonReader): Int =
    if (r.peek() == JsonToken.NUMBER) r.nextDouble().toInt() else { r.skipValue(); 0 }

/**
 * Cuenta una institución fuera del hilo principal. Quien descarga varias instituciones puede lanzar
 * una por cada descarga con async, y se cuentan en paralelo.
 */
suspend fun contarEnSegundoPlano(
    estudiantesJson: String,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    contar: (String) -> IntArray = ::contarNiveles
): IntArray = withContext(dispatcher) { contar(estudiantesJson) }
