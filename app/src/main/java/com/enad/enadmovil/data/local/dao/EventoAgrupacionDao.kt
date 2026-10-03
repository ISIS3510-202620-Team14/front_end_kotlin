package com.enad.enadmovil.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.enad.enadmovil.data.local.entity.EventoAgrupacionEntity
import com.enad.enadmovil.data.local.entity.SyncStatus

@Dao
interface EventoAgrupacionDao {
    @Insert suspend fun insertar(evento: EventoAgrupacionEntity)

    @Query("SELECT * FROM eventos_agrupacion WHERE docenteUid = :uid AND subject = :materia")
    suspend fun eventos(uid: String, materia: String): List<EventoAgrupacionEntity>

    @Query("SELECT * FROM eventos_agrupacion WHERE docenteUid = :uid AND syncStatus = 'PENDING' ORDER BY creadoEn")
    suspend fun pendientes(uid: String): List<EventoAgrupacionEntity>

    @Query("UPDATE eventos_agrupacion SET intentos = intentos + 1 WHERE id = :id")
    suspend fun sumarIntento(id: String)

    @Query("UPDATE eventos_agrupacion SET syncStatus = :estado, ultimoError = :error WHERE id = :id")
    suspend fun marcar(id: String, estado: SyncStatus, error: String?)
}
