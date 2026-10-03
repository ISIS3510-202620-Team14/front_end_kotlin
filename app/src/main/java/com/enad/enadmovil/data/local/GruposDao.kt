package com.enad.enadmovil.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.enad.enadmovil.data.local.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface GruposDao {
    @Transaction
    @Query("SELECT * FROM grupos WHERE eliminado = 0")
    fun observarGrupos(): Flow<List<GrupoConNinos>>

    @Query("SELECT COUNT(*) FROM grupos")
    suspend fun contarGrupos(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarNinos(ninos: List<NinoEntity>)

    @Insert
    suspend fun insertarGrupo(grupo: GrupoEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarCrossRefs(refs: List<GrupoNinoCrossRef>)

    @Query("DELETE FROM grupo_nino_cross_ref WHERE grupoId = :grupoId AND ninoId = :ninoId")
    suspend fun quitarNinoDeGrupo(grupoId: Int, ninoId: Int)

    @Query("UPDATE grupos SET nombre = :nuevoNombre WHERE id = :grupoId")
    suspend fun actualizarNombre(grupoId: Int, nuevoNombre: String)

    @Query("UPDATE grupos SET docente = :nuevoDocente WHERE id = :grupoId")
    suspend fun actualizarDocente(grupoId: Int, nuevoDocente: String)

    @Query("DELETE FROM grupos WHERE id = :grupoId")
    suspend fun eliminarGrupo(grupoId: Int)

    @Query("SELECT * FROM grupos WHERE syncStatus = 'PENDING' AND eliminado = 0 AND remoteId IS NULL")
    suspend fun pendientesDeCrear(): List<GrupoEntity>

    @Query("SELECT * FROM grupos WHERE eliminado = 1")
    suspend fun pendientesDeEliminar(): List<GrupoEntity>

    @Query("SELECT remoteId FROM grupos WHERE remoteId IS NOT NULL")
    suspend fun idsRemotos(): List<String>

    @Query("UPDATE grupos SET remoteId = :remoteId, syncStatus = 'SYNCED' WHERE id = :grupoId")
    suspend fun marcarSubido(grupoId: Int, remoteId: String)

    @Query("UPDATE grupos SET syncStatus = 'FAILED' WHERE id = :grupoId")
    suspend fun marcarFallido(grupoId: Int)

    @Query("UPDATE grupos SET eliminado = 1 WHERE id = :grupoId")
    suspend fun marcarEliminado(grupoId: Int)
}
