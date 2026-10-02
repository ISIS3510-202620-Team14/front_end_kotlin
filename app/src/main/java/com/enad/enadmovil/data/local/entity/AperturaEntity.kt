package com.enad.enadmovil.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "aperturas", indices = [Index(value = ["docenteUid", "semana"])])
data class AperturaEntity(
    @PrimaryKey val id: String,      // clientId: makes retried uploads idempotent on the server
    val docenteUid: String,
    val abiertaEn: Long,             // epoch millis
    val semana: String,              // Monday of the week, yyyy-MM-dd
    val platform: String,
    val appVersion: String,
    val syncStatus: SyncStatus
)