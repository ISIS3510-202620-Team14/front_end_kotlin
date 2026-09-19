package com.enad.enadmovil.domain.model

import java.time.LocalDate
import kotlin.math.ceil

data class DiaSesion(val fecha: LocalDate, val horasProgramadas: Double, val horasReales: Double? = null) {
    fun nivelIntensidad(maxHorasReales: Double): Int {
        val horas = horasReales
        if (horas == null || horas <= 0 || maxHorasReales <= 0) return 0
        val proporcion = horas / maxHorasReales
        return ceil(proporcion * 4).toInt().coerceIn(1,4)
    }
}