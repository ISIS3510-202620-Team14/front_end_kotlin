package com.enad.enadmovil.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface GruposDao {
    @Transaction
    @Query("SELECT * FROM grupos")
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
}
