package com.enad.enadmovil.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Un grupo recién creado y el método con que se armó. Se sube al backend para la recomendación de la BQ 14. */
@Entity(tableName = "eventos_agrupacion")
data class EventoAgrupacionEntity(
    @PrimaryKey val id: String,
    val docenteUid: String,
    val subject: String,
    val classSize: Int,
    val method: String,
    val creadoEn: Long,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val intentos: Int = 0,
    val ultimoError: String? = null
)
