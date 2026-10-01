package com.enad.enadmovil.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.enad.enadmovil.data.local.entity.EstudianteEntity
import com.enad.enadmovil.data.local.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface EstudianteDao {
    @Upsert suspend fun upsert(e: EstudianteEntity)
    @Upsert suspend fun upsertAll(items: List<EstudianteEntity>)

    @Query("SELECT * FROM estudiantes WHERE schoolId = :schoolId ORDER BY fullName COLLATE NOCASE")
    fun observar(schoolId: String): Flow<List<EstudianteEntity>>

    @Query("SELECT * FROM estudiantes WHERE id = :id")
    suspend fun porId(id: String): EstudianteEntity?

    @Query("SELECT * FROM estudiantes WHERE schoolId = :schoolId AND code = :code LIMIT 1")
    suspend fun porCodigo(schoolId: String, code: String): EstudianteEntity?

    @Query("SELECT * FROM estudiantes WHERE schoolId = :schoolId AND fullName = :nombre COLLATE NOCASE AND grade = :grade LIMIT 1")
    suspend fun porNombre(schoolId: String, nombre: String, grade: Int): EstudianteEntity?

    @Query("SELECT * FROM estudiantes WHERE docenteUid = :uid AND syncStatus = 'PENDING' AND grade IS NOT NULL")
    suspend fun pendientes(uid: String): List<EstudianteEntity>

    @Query("UPDATE estudiantes SET syncStatus = :estado, code = COALESCE(:code, code) WHERE id = :id")
    suspend fun actualizarSync(id: String, estado: SyncStatus, code: String?)

    @Query("DELETE FROM estudiantes WHERE id = :id")
    suspend fun borrar(id: String)
}