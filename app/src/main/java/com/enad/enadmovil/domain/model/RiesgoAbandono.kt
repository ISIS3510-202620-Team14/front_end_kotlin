package com.enad.enadmovil.domain.model

data class RiesgoAbandono(
    val enRiesgo: Boolean,
    val semanaPasada: Int,
    val semanaHabitual: Double
) {
    companion object {
        const val SEMANAS_BASE = 3
        const val APERTURAS_MINIMAS = 1.0

        val SIN_RIESGO = RiesgoAbandono(enRiesgo = false, semanaPasada = 0, semanaHabitual = 0.0)

        fun desdeAperturas(aperturasPorSemana: List<Int>): RiesgoAbandono {
            if (aperturasPorSemana.size < SEMANAS_BASE + 1) return SIN_RIESGO
            val ultimas = aperturasPorSemana.takeLast(SEMANAS_BASE + 1)
            val habitual = ultimas.dropLast(1).average()
            val pasada = ultimas.last()
            if (habitual < APERTURAS_MINIMAS) return RiesgoAbandono(false, pasada, habitual)
            return RiesgoAbandono(pasada <= habitual / 2, pasada, habitual)
        }
    }
}