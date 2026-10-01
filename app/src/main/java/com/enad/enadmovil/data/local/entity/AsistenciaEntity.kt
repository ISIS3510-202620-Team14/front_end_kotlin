package com.enad.enadmovil.data.local.entity

import androidx.room.Entity

/** Estados tal como los espera PUT /students/{id}/attendance. */
object EstadoRemoto {
    const val VINO = "vino"
    const val NO_VINO = "no_vino"
    const val SIN_REGISTRO = "sin_registro"
}

@Entity(tableName = "asistencias", primaryKeys = ["estudianteId", "fecha"])
data class AsistenciaEntity(
    val estudianteId: String,
    val fecha: String,                // yyyy-MM-dd
    val estado: String,               // EstadoRemoto
    val docenteUid: String,
    val syncStatus: SyncStatus
)