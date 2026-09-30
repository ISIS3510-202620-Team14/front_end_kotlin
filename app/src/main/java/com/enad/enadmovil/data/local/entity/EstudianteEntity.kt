package com.enad.enadmovil.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "estudiantes")
data class EstudianteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val schoolId: String,
    val docenteUid: String,
    val nombre: String,
    val codigo: String,
    val codigoGenerado: Boolean,      // true = lo inventó la app (incremental)
    val grado: String?,
    val edad: Int?,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)