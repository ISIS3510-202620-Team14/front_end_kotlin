package com.enad.enadmovil.domain.model

import kotlin.math.abs

/** Cómo arma el docente los grupos: la app reparte a los niños o los elige él a mano. */
enum class MetodoAgrupacion(val clave: String) {
    AUTOMATICO("automatic"),
    MANUAL("manual");

    companion object {
        fun deClave(clave: String): MetodoAgrupacion? = values().firstOrNull { it.clave == clave }
    }
}

/** Un grupo ya creado: la materia, cuántos niños había y con qué método se armó. */
data class EventoAgrupacion(val materia: String, val tamano: Int, val metodo: MetodoAgrupacion)

data class Recomendacion(val metodo: MetodoAgrupacion, val confianza: Double, val muestra: Int) {
    /** Con menos eventos parecidos que esto no se muestra la insignia "Recomendado". */
    val tieneDatosSuficientes: Boolean get() = muestra >= MUESTRA_MINIMA

    companion object {
        const val MUESTRA_MINIMA = 3
    }
}

/** BQ 14: según la materia y el tamaño del salón, predice el método que más se usó en salones parecidos. */
object RecomendadorAgrupacion {

    fun recomendar(eventos: List<EventoAgrupacion>, materia: String, tamano: Int): Recomendacion {
        val tolerancia = toleranciaDeTamano(tamano)
        val parecidos = eventos.filter { it.materia == materia && abs(it.tamano - tamano) <= tolerancia }
        if (parecidos.isEmpty()) return Recomendacion(MetodoAgrupacion.AUTOMATICO, 0.5, 0)

        val proporcionAutomatico = parecidos.count { it.metodo == MetodoAgrupacion.AUTOMATICO }.toDouble() / parecidos.size
        return if (proporcionAutomatico >= 0.5) {
            Recomendacion(MetodoAgrupacion.AUTOMATICO, proporcionAutomatico, parecidos.size)
        } else {
            Recomendacion(MetodoAgrupacion.MANUAL, 1.0 - proporcionAutomatico, parecidos.size)
        }
    }

    /** Un salón de 8 se parece a uno de 11, pero uno de 30 solo a uno de 22 a 38. */
    fun toleranciaDeTamano(tamano: Int): Int = when {
        tamano <= 10 -> 3
        tamano <= 25 -> 5
        else -> 8
    }
}
