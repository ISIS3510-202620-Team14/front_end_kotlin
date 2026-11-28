package com.enad.enadmovil

import android.os.Debug
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.enad.enadmovil.data.niveles.contarNiveles
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * MICRO-OPTIMIZACIÓN (Sprint 4): mide en un dispositivo Android el conteo de niveles de una
 * institución (contarNiveles) con JSON de la misma forma que GET /students. Registra en Logcat
 * (tag niveles-bench) el tiempo por institución y lo que asigna el runtime de ART, para comparar
 * el antes y el después de la optimización.
 */
@RunWith(AndroidJUnit4::class)
class ConteoNivelesBenchmark {

    private fun nivel(orden: Int): Any =
        if (orden == 0) JSONObject.NULL
        else JSONObject().put("clave", "nivel_$orden").put("etiqueta", "Nivel $orden").put("orden", orden)
            .put("date", "2026-11-0${1 + orden % 9}T10:00:00.000Z")

    /** Una institución con [estudiantes] estudiantes, como la responde el backend. */
    private fun institucionJson(estudiantes: Int, semilla: Int): String {
        val arreglo = JSONArray()
        for (i in 0 until estudiantes) {
            val n = i + semilla
            arreglo.put(
                JSONObject()
                    .put("id", "est_$n")
                    .put("name", "Estudiante número $n")
                    .put("schoolId", "escuela_$semilla")
                    .put("grade", 3 + n % 3)
                    .put("gender", if (n % 2 == 0) "F" else "M")
                    .put("age", 8 + n % 4)
                    .put("sample", 1 + n % 4)
                    .put("campus", "Sede ${n % 3}")
                    .put("retired", n % 17 == 0)
                    .put("provisional", false)
                    .put("levels", JSONObject().put("lectura", nivel(n % 7)).put("matematicas", nivel((n / 2) % 7)))
            )
        }
        return JSONObject().put("students", arreglo).toString()
    }

    /** Referencia para comprobar que el resultado no cambia: el conteo hecho con JSONObject, campo por campo. */
    private fun referencia(json: String): IntArray {
        val conteos = IntArray(42)
        val estudiantes = JSONObject(json).getJSONArray("students")
        for (i in 0 until estudiantes.length()) {
            val e = estudiantes.getJSONObject(i)
            if (e.optBoolean("retired")) continue
            val grado = e.optInt("grade") - 3
            if (grado !in 0..2) continue
            val niveles = e.optJSONObject("levels")
            listOf("lectura", "matematicas").forEachIndexed { m, materia ->
                var orden = niveles?.optJSONObject(materia)?.optInt("orden", 0) ?: 0
                if (orden !in 0..6) orden = 0
                conteos[(m * 3 + grado) * 7 + orden]++
            }
        }
        return conteos
    }

    private fun bytesAsignados() = Debug.getRuntimeStat("art.gc.bytes-allocated")?.toLongOrNull() ?: 0L
    private fun gcs() = Debug.getRuntimeStat("art.gc.gc-count")?.toLongOrNull() ?: 0L

    @Test
    fun contarNivelesDeVariasInstituciones() {
        val instituciones = List(20) { institucionJson(estudiantes = 400, semilla = it * 1000) }

        // El resultado debe ser el mismo antes y después de la optimización
        instituciones.forEach { assertArrayEquals(referencia(it), contarNiveles(it)) }

        // Calentamiento: que el JIT compile el código antes de medir
        repeat(3) { instituciones.forEach { contarNiveles(it) } }

        val rondas = 5
        val tiempos = LongArray(rondas)
        val bytesAntes = bytesAsignados()
        val gcAntes = gcs()
        var total = 0
        for (r in 0 until rondas) {
            val inicio = System.nanoTime()
            for (json in instituciones) total += contarNiveles(json).sum()
            tiempos[r] = (System.nanoTime() - inicio) / instituciones.size
        }
        val bytes = bytesAsignados() - bytesAntes
        val gc = gcs() - gcAntes
        val conteos = rondas * instituciones.size

        tiempos.sort()
        Log.i(TAG, "instituciones=${instituciones.size} · estudiantes por institución=400 · KB de JSON=${instituciones[0].length / 1024}")
        Log.i(TAG, "tiempo por institución: mediana=${tiempos[rondas / 2] / 1_000} µs · min=${tiempos[0] / 1_000} µs · max=${tiempos[rondas - 1] / 1_000} µs")
        Log.i(TAG, "memoria asignada por institución=${bytes / conteos / 1024} KB · GC durante la medición=$gc")
        assertEquals(true, total > 0)
    }

    private companion object {
        const val TAG = "niveles-bench"
    }
}
