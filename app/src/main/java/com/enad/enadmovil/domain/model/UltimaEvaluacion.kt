package com.enad.enadmovil.domain.model

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * BQ tipo 2: cuántos días han pasado desde la última evaluación de un estudiante en una materia.
 */

object UltimaEvaluacion  {
    const val DIAS_LIMITE = 30

    private val COLOMBIA: ZoneId = ZoneId.of("America/Bogota")

    fun diasDesde(fechaIso: String?, hoy: LocalDate): Int? {
        if (fechaIso.isNullOrBlank()) return null
        val fecha = runCatching { OffsetDateTime.parse(fechaIso).atZoneSameInstant(COLOMBIA).toLocalDate() }.getOrNull()
            ?: runCatching { LocalDate.parse(fechaIso.take(10))}.getOrNull()
            ?: return null
        return ChronoUnit.DAYS.between(fecha, hoy).toInt().coerceAtLeast(0)
    }

    fun pendiente(dias: Int?, limite: Int = DIAS_LIMITE): Boolean = dias == null || dias > limite
    fun atrasados(fechasIso: List<String?>, hoy: LocalDate, limite: Int = DIAS_LIMITE): Int =
        fechasIso.count { fecha -> diasDesde(fecha, hoy)?.let { it > limite} == true}

    fun texto(dias: Int): String = when (dias) {
        0 -> "evaluado hoy"
        1 -> "evaluado ayer"
        else -> "hace $dias días"
    }
}