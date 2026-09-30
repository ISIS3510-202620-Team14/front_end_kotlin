package com.enad.enadmovil.data.local.entity

import androidx.room.Entity

@Entity(tableName = "asistencias", primaryKeys = ["grupoId", "estudianteId", "fecha"])
data class AsistenciaEntity(
    val grupoId: String,              // el grupo es independiente del curso/grado
    val estudianteId: String,
    val fecha: String,                // yyyy-MM-dd
    val presente: Boolean,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)