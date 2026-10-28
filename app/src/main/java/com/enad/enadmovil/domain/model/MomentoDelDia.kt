package com.enad.enadmovil.domain.model

import java.time.LocalTime

/** Lo que el docente lleva hecho hoy. Sale de Room: horario de los grupos, asistencia, horas y presencia en la sede. */
data class ResumenDelDia(
    val grupo: String?,               // grupo que toca hoy (el confirmado o el primero del horario)
    val horasPlaneadas: Double,       // suma del horario de hoy
    val marcasAsistencia: Int,        // estudiantes con asistencia registrada hoy
    val horasReportadas: Boolean,
    val horasEnSede: Double?,         // de la primera a la última vez que se vio en la sede; null = sin dato
    // false = el horario nunca se descargó (primer uso sin internet): no se sabe si hoy hay clase
    val horarioDescargado: Boolean = true
)

/** Siguiente paso que la tarjeta de Hoy le propone al docente. */
enum class AccionSugerida { ASISTENCIA, EVALUACION, HORAS, ACTIVIDADES, NINGUNA }

/** En qué va el día del docente. Cambia con la hora, el horario y lo que ya registró. */
enum class MomentoDelDia(val clave: String, val accion: AccionSugerida) {
    SIN_HORARIO("no_schedule", AccionSugerida.NINGUNA),
    SIN_CLASE("no_class", AccionSugerida.ACTIVIDADES),
    INICIO("start", AccionSugerida.ASISTENCIA),
    EN_CLASE("in_class", AccionSugerida.EVALUACION),
    CIERRE("wrap_up", AccionSugerida.HORAS),
    SIN_ASISTENCIA("missed_attendance", AccionSugerida.ASISTENCIA),
    LISTO("done", AccionSugerida.NINGUNA);

    companion object {
        /** Desde esta hora la jornada se da por terminada aunque no haya dato de la sede. */
        val FIN_JORNADA: LocalTime = LocalTime.of(17, 0)

        /**
         * Context-aware: decide el momento sin preguntarle nada al docente.
         * - Sin horario descargado no se puede saber si hay clase: se pide conectarse una vez.
         * - Sin horas en el horario de hoy: no hay clase.
         * - Sin asistencia: al inicio pide la asistencia; al final del día avisa que faltó.
         * - Con asistencia y sin horas: en clase hasta que pasa el tiempo planeado en la sede
         *   (o llega el fin de la jornada), y entonces pide registrar las horas.
         * - Con asistencia y horas: el día está listo.
         */
        fun de(resumen: ResumenDelDia, hora: LocalTime): MomentoDelDia {
            val terminoJornada = !hora.isBefore(FIN_JORNADA)
            return when {
                resumen.marcasAsistencia > 0 && resumen.horasReportadas -> LISTO
                !resumen.horarioDescargado -> SIN_HORARIO
                resumen.horasPlaneadas <= 0.0 -> if (resumen.horasReportadas) LISTO else SIN_CLASE
                resumen.marcasAsistencia == 0 -> if (terminoJornada) SIN_ASISTENCIA else INICIO
                terminoJornada -> CIERRE
                (resumen.horasEnSede ?: 0.0) >= resumen.horasPlaneadas -> CIERRE
                else -> EN_CLASE
            }
        }
    }
}
