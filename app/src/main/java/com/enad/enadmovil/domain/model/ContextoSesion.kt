package com.enad.enadmovil.domain.model

import java.time.DayOfWeek

/** De dónde salió un valor: sugerido por la app o elegido/escrito por el docente. */
object OrigenValor {
    const val SUGERIDO = "suggested"
    const val MANUAL = "manual"
}
/** Un grupo del docente visto desde un día concreto. */
data class GrupoDelDia(val id: String, val nombre: String, val materia: String, val sede: String?, val horasPlaneadas: Double?)

/** Sugerencia de horas realizadas de un día. Nunca supera la permanencia en la sede. */
data class SugerenciaHoras(val horas: Double, val permanenciaHoras: Double)

/** Horario semanal guardado como texto: "1=1.5;3=1.5" (día ISO = horas planeadas). */
object Horario {
    fun codificar(dias: List<Pair<Int, Double>>): String = dias.joinToString(";") { (dia, horas) -> "$dia=$horas"  }

    fun horasDelDia(horario: String, dia: DayOfWeek): Double? = horario.split(";").firstNotNullOfOrNull { parte ->
        val (numero, horas) = parte.split("=").takeIf { it.size == 2 } ?: return@firstNotNullOfOrNull null
        if (numero.toIntOrNull() == dia.value) horas.toDoubleOrNull() else null
    }
}