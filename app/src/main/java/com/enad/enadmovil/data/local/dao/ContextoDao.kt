package com.enad.enadmovil.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.enad.enadmovil.data.local.entity.ContextoDiaEntity
import com.enad.enadmovil.data.local.entity.GrupoHorarioEntity
import com.enad.enadmovil.data.local.entity.PresenciaSedeEntity
import com.enad.enadmovil.data.local.entity.ReporteHorasEntity
import com.enad.enadmovil.data.local.entity.SedeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContextoDao {
    @Query("DELETE FROM sedes") suspend fun borrarSedes()
    @Upsert suspend fun guardarSedes(sedes: List<SedeEntity>)

    @Query("SELECT * FROM sedes WHERE lat IS NOT NULL AND lng IS NOT NULL")
    suspend fun sedesUbicadas(): List<SedeEntity>

    @Query("SELECT * FROM sedes WHERE id = :id")
    suspend fun sede(id: String): SedeEntity?

    @Query("DELETE FROM grupos_horario WHERE docenteUid = :uid")
    suspend fun borrarGrupos(uid: String)
    @Upsert suspend fun guardarGrupos(grupos: List<GrupoHorarioEntity>)

    @Query("SELECT * FROM grupos_horario WHERE docenteUid = :uid ORDER BY nombre COLLATE NOCASE")
    suspend fun grupos(uid: String): List<GrupoHorarioEntity>

    @Query("SELECT * FROM grupos_horario WHERE id = :id")
    suspend fun grupo(id: String): GrupoHorarioEntity?

    //Presencia y contexto del dia
    @Query("SELECT * FROM presencia_sede WHERE docenteUid = :uid AND fecha = :fecha AND sedeId = :sedeId")
    suspend fun presencia(uid: String, fecha: String, sedeId: String): PresenciaSedeEntity?

    @Query("SELECT * FROM presencia_sede WHERE docenteUid = :uid AND fecha = :fecha")
    suspend fun presenciasDelDia(uid: String, fecha: String): List<PresenciaSedeEntity>

    @Upsert suspend fun guardarPresencia(presencia: PresenciaSedeEntity)

    @Query("SELECT * FROM contexto_dia WHERE docenteUid = :uid AND fecha = :fecha")
    suspend fun contexto(uid: String, fecha: String): ContextoDiaEntity?

    @Upsert suspend fun guardarContexto(contexto: ContextoDiaEntity)

    //Horas
    @Upsert suspend fun guardarReporte(reporte: ReporteHorasEntity)

    @Query("SELECT * FROM reportes_horas WHERE docenteUid = :uid AND fecha BETWEEN :desde AND :hasta")
    fun observarReportes(uid: String, desde: String, hasta: String): Flow<List<ReporteHorasEntity>>

    @Query("SELECT * FROM reportes_horas WHERE docenteUid = :uid AND fecha = :fecha")
    suspend fun reporte(uid: String, fecha: String): ReporteHorasEntity?

    /** Evidencia de clase: asistencia registrada por el docente ese día. */
    @Query("SELECT COUNT(*) FROM asistencias WHERE docenteUid = :uid AND fecha = :fecha AND estado != 'sin_registro'")
    suspend fun asistenciasDelDia(uid: String, fecha: String): Int
}