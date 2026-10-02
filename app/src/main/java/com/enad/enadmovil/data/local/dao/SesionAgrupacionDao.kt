package com.enad.enadmovil.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.enad.enadmovil.data.local.entity.SesionAgrupacionEntity
import com.enad.enadmovil.data.local.entity.SyncStatus

@Dao
interface SesionAgrupacionDao {
    @Insert suspend fun insertar(s: SesionAgrupacionEntity)

    @Query("SELECT * FROM sesiones_agrupacion WHERE docenteUid = :uid AND syncStatus = 'PENDING' ORDER BY creadaEn")
    suspend fun pendientes(uid: String): List<SesionAgrupacionEntity>

    @Query("UPDATE sesiones_agrupacion SET intentos = intentos + 1 WHERE id = :id")
    suspend fun sumarIntento(id: String)

    @Query("UPDATE sesiones_agrupacion SET syncStatus = :estado, sincronizadaEn = :cuando, ultimoError = :error WHERE id = :id")
    suspend fun marcar(id: String, estado: SyncStatus, cuando: Long?, error: String?)
}