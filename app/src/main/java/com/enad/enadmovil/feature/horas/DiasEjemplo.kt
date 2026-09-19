package com.enad.enadmovil.feature.horas

import androidx.compose.runtime.mutableStateListOf
import com.enad.enadmovil.domain.model.DiaSesion
import java.time.LocalDate
import kotlin.random.Random

private val horasPlaneadasPorDia = listOf(1.5, 1.0, 1.5, 1.0, 1.0)

fun diasEjemploUltimasSemanas(semanas: Int = 12, hoy: LocalDate = LocalDate.now()): List<DiaSesion> {
    val random = Random(7)
    val lunesDeEstaSemana = hoy.minusDays((hoy.dayOfWeek.value - 1).toLong())
    val primerLunes = lunesDeEstaSemana.minusWeeks(((semanas - 1).toLong()))

    val dias = mutableStateListOf<DiaSesion>()
    for (semana in 0 until semanas) {
        val lunes = primerLunes.plusWeeks(semana.toLong())
        for (diasSemana in 0 until 5) {
            val fecha = lunes.plusDays(diasSemana.toLong())
            val planeadas = horasPlaneadasPorDia[diasSemana]
            if(fecha.isAfter(hoy)) continue
            if(fecha.isEqual(hoy)) {
                dias.add(DiaSesion(fecha = fecha, horasProgramadas = planeadas))
                continue
            }
            val roll = random.nextDouble()
            val reales = when {
                roll < 0.12 -> 0.0
                roll < 0.35 -> planeadas * 0.5
                roll < 0.75 -> planeadas
                else -> planeadas * 1.3
            }
            dias.add(DiaSesion(fecha = fecha, horasProgramadas = planeadas, horasReales = reales))
        }
    }
    return dias
}