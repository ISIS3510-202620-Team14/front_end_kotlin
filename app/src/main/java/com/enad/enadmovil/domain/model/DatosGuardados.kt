package com.enad.enadmovil.domain.model

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object DatosGuardados {

    private val ZONA = ZoneId.of("America/Bogota")
    private val MESES = listOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
    )


    fun cuando(guardadoEnMs: Long, ahoraMs: Long = System.currentTimeMillis()): String {
        val guardado = Instant.ofEpochMilli(guardadoEnMs).atZone(ZONA)
        val hoy = Instant.ofEpochMilli(ahoraMs).atZone(ZONA).toLocalDate()
        val dia = when (ChronoUnit.DAYS.between(guardado.toLocalDate(), hoy)) {
            0L -> "hoy"
            1L -> "ayer"
            else -> "el ${guardado.dayOfMonth} de ${MESES[guardado.monthValue - 1]}"
        }
        val hora = if (guardado.hour % 12 == 0) 12 else guardado.hour % 12
        val minutos = guardado.minute.toString().padStart(2, '0')
        val franja = if (guardado.hour < 12) "a. m." else "p. m."
        return "$dia ${if (hora == 1) "a la" else "a las"} $hora:$minutos $franja"
    }
}
