package com.enad.enadmovil.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

object Semana {
    // Week key = Monday of that week (yyyy-MM-dd), same format as the groupings report
    fun id(fecha: LocalDate = LocalDate.now()): String =
        fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()
}