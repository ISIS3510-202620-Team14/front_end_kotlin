package com.enad.enadmovil.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Sede descargada del backend (schools.campuses). Se guarda para detectar la sede sin señal. */
@Entity(tableName = "sedes")
data class SedeEntity(@PrimaryKey val id: String, val schoolId: String, val campusId: String, val nombre: String, val escuela: String, val lat: Double?, val lng: Double?) // "$schoolId/$campusId"

/** Grupo del docente descargado del backend, con su sede y su horario semanal. */
@Entity(tableName = "grupos_horario")
data class GrupoHorarioEntity(@PrimaryKey val id: String, val docenteUid: String, val nombre: String, val materia: String, val schoolId: String, val campusId: String?, val horario: String)

/** Presencia en una sede por día. Solo horas: nunca se guardan coordenadas del docente. */
@Entity(tableName = "presencia_sede", primaryKeys = ["docenteUid", "fecha", "sedeId"])
data class PresenciaSedeEntity(val docenteUid: String, val fecha: String, val sedeId: String, val primeraVez: Long, val ultimaVez: Long)

/** Grupo con el que el docente inició el día y si lo eligió por sugerencia o a mano. */
@Entity(tableName = "contexto_dia", primaryKeys = ["docenteUid", "fecha"])
data class ContextoDiaEntity(val docenteUid: String, val fecha: String, val grupoId: String, val origen: String, val confirmadoEn: Long)

/** Horas realizadas de un día, con el origen del valor (sugerido o escrito por el docente). */
@Entity(tableName = "reportes_horas", primaryKeys = ["docenteUid", "fecha"])
data class ReporteHorasEntity(val docenteUid: String, val fecha: String, val horasPlaneadas: Double, val horasRealizadas: Double, val origen: String, val motivo: String?, val presenteEnSede: Boolean, val guardadoEn: Long, val syncStatus: SyncStatus = SyncStatus.PENDING, val intentos: Int = 0, val ultimoError: String? = null)

