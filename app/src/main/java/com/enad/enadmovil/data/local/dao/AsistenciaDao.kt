package com.enad.enadmovil.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.enad.enadmovil.data.local.entity.AsistenciaEntity
import com.enad.enadmovil.data.local.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface AsistenciaDao {
    @Upsert suspend fun upsert(a: AsistenciaEntity)
    @Upsert suspend fun upsertAll(items: List<AsistenciaEntity>)

    @Query("SELECT * FROM asistencias WHERE fecha = :fecha")
    fun observarDelDia(fecha: String): Flow<List<AsistenciaEntity>>

    // Las fechas yyyy-MM-dd se ordenan como texto, así que BETWEEN sirve para una semana.
    @Query("SELECT * FROM asistencias WHERE fecha BETWEEN :desde AND :hasta")
    fun observarRango(desde: String, hasta: String): Flow<List<AsistenciaEntity>>

    @Query("SELECT * FROM asistencias WHERE fecha = :fecha")
    suspend fun delDia(fecha: String): List<AsistenciaEntity>

    @Query("SELECT * FROM asistencias WHERE estudianteId = :id AND fecha = :fecha")
    suspend fun buscar(id: String, fecha: String): AsistenciaEntity?

    @Query("SELECT * FROM asistencias WHERE docenteUid = :uid AND syncStatus = 'PENDING'")
    suspend fun pendientes(uid: String): List<AsistenciaEntity>

    // Solo marca como sincronizada si el estado no cambió mientras se subía.
    @Query("UPDATE asistencias SET syncStatus = :sync WHERE estudianteId = :id AND fecha = :fecha AND estado = :estado")
    suspend fun actualizarSync(id: String, fecha: String, estado: String, sync: SyncStatus)

    @Query("UPDATE OR REPLACE asistencias SET estudianteId = :nuevo WHERE estudianteId = :viejo")
    suspend fun reasignar(viejo: String, nuevo: String)

    @Query(
        "SELECT (SELECT COUNT(*) FROM estudiantes WHERE docenteUid = :uid AND syncStatus = 'PENDING') + " +
                "(SELECT COUNT(*) FROM asistencias WHERE docenteUid = :uid AND syncStatus = 'PENDING')"
    )
    fun contarPendientes(uid: String): Flow<Int>
}