package com.enad.enadmovil.data.niveles

import com.enad.enadmovil.domain.model.NivelesInstitucion
import org.json.JSONObject

/**
 * Lee la respuesta de GET /students de una institución: cada estudiante trae grade, retired y
 * levels.{materia}.orden (1..6, null si todavía no tiene nivel).
 */
fun leerEstudiantes(estudiantesJson: String): List<EstudianteNivel> {
    val arreglo = JSONObject(estudiantesJson).optJSONArray("students") ?: return emptyList()
    val estudiantes = ArrayList<EstudianteNivel>(arreglo.length())
    for (i in 0 until arreglo.length()) {
        val o = arreglo.optJSONObject(i) ?: continue
        val niveles = o.optJSONObject("levels")
        val ordenes = HashMap<String, Int?>(NivelesInstitucion.MATERIAS.size)
        for (materia in NivelesInstitucion.MATERIAS) {
            val nivel = niveles?.optJSONObject(materia)
            ordenes[materia] = if (nivel == null || nivel.isNull("orden")) null else nivel.optInt("orden", 0)
        }
        estudiantes += EstudianteNivel(
            grado = o.optInt("grade", 0),
            retirado = o.optBoolean("retired", false),
            ordenPorMateria = ordenes
        )
    }
    return estudiantes
}
