package com.enad.enadmovil.data.telemetria

import com.enad.enadmovil.data.remote.FirebaseModule
import com.google.firebase.firestore.FieldValue
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * BQ Sprint 3 (tipo 1): por cada sincronización de asistencia se guarda en "events" cuántas marcas
 * se enviaron y cuántas no se aplicaron porque otro celular ya había registrado otro valor.
 * Va un evento por institución, semana y conectividad al marcar; el backend los suma en
 * GET /attendanceConflicts/report.
 */
object EventosSincronizacion {

    data class Conteo(val enviadas: Int = 0, val conflictos: Int = 0)

    /** clave: (schoolId, fecha de la marca yyyy-MM-dd, conectividad al marcar). */
    fun registrar(uid: String, conteos: Map<Triple<String, String, String>, Conteo>) {
        val porSemana = conteos.entries.groupBy({ (k, _) -> Triple(k.first, lunesDe(k.second), k.third) }, { it.value })
        for ((clave, lista) in porSemana) {
            val (schoolId, semana, conectividad) = clave
            val evento = mapOf(
                "name" to "attendance_sync",
                "teacherId" to uid,
                "platform" to InfoDispositivo.PLATAFORMA,
                "schoolId" to schoolId,
                "week" to semana,
                "connectivity" to conectividad,
                "sent" to lista.sumOf { it.enviadas },
                "conflicts" to lista.sumOf { it.conflictos },
                "createdAt" to FieldValue.serverTimestamp()
            )
            // Sin esperar respuesta: si falla, la asistencia ya quedó enviada y solo se pierde el dato
            runCatching { FirebaseModule.firestore.collection("events").add(evento) }
        }
    }

    private fun lunesDe(fecha: String): String =
        runCatching { LocalDate.parse(fecha).with(DayOfWeek.MONDAY).toString() }.getOrDefault(fecha)
}
