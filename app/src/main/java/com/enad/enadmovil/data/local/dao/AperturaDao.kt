package com.enad.enadmovil.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.enad.enadmovil.data.local.entity.AperturaEntity
import com.enad.enadmovil.data.local.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface AperturaDao {
    @Insert suspend fun insertar(a: AperturaEntity)

    // Read locally so the counter works offline
    @Query("SELECT COUNT(*) FROM aperturas WHERE docenteUid = :uid AND semana = :semana")
    fun observarDeLaSemana(uid: String, semana: String): Flow<Int>

    @Query("SELECT MAX(abiertaEn) FROM aperturas WHERE docenteUid = :uid")
    suspend fun ultimaApertura(uid: String): Long?

    @Query("SELECT * FROM aperturas WHERE docenteUid = :uid AND syncStatus = 'PENDING' ORDER BY abiertaEn")
    suspend fun pendientes(uid: String): List<AperturaEntity>

    @Query("UPDATE aperturas SET syncStatus = :estado WHERE id IN (:ids)")
    suspend fun marcar(ids: List<String>, estado: SyncStatus)
}