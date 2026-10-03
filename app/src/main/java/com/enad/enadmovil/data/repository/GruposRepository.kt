package com.enad.enadmovil.data.repository

import android.content.Context
import com.enad.enadmovil.data.local.EnadDatabase
import com.enad.enadmovil.data.local.GrupoEntity
import com.enad.enadmovil.data.local.GrupoNinoCrossRef
import com.enad.enadmovil.data.local.NinoEntity
import com.enad.enadmovil.data.local.entity.SyncStatus
import com.enad.enadmovil.data.mapper.aGrupo
import com.enad.enadmovil.data.sync.SyncScheduler
import com.enad.enadmovil.domain.model.AreaMateria
import com.enad.enadmovil.domain.model.Grupo
import com.enad.enadmovil.domain.model.MetodoAgrupacion
import com.enad.enadmovil.domain.model.Nino
import com.enad.enadmovil.domain.model.Recomendacion
import com.enad.enadmovil.feature.grupos.ninosDeEjemplo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository: grupos de agrupación guardados en Room. El ViewModel solo ve modelos de dominio
 * (Grupo, Nino); las entidades, el DAO y la sincronización se quedan en esta capa. Al crear un
 * grupo también registra la sesión de agrupación (BQ tipo 1) y el método usado (BQ 14).
 */
class GruposRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dao = EnadDatabase.obtener(appContext).gruposDao()
    private val sesionesAgrupacion = SesionAgrupacionRepository(appContext)
    private val agrupacion = AgrupacionInteligenteRepository(appContext)
    private val gruposRemotos = GruposRemotosRepository(appContext)

    fun observarGrupos(area: AreaMateria): Flow<List<Grupo>> =
        dao.observarGrupos().map { grupos -> grupos.filter { it.grupo.area == area }.map { it.aGrupo() } }

    /** La primera vez que se abre la app, deja tres grupos de ejemplo para que la pantalla no esté vacía. */
    suspend fun sembrarSiVacio() {
        if (dao.contarGrupos() > 0) return
        dao.insertarNinos(ninosDeEjemplo.map { NinoEntity(it.id, it.nombre, it.nivel, it.grado) })
        crearEnRoom(GrupoEntity(nombre = "Grupo Abejitas", docente = "Yo", area = AreaMateria.MATEMATICAS), listOf(2, 6))
        crearEnRoom(GrupoEntity(nombre = "Grupo Colibríes", docente = "Prof. Nelson", area = AreaMateria.MATEMATICAS), listOf(1, 7))
        crearEnRoom(GrupoEntity(nombre = "Grupo Tortugas", docente = "Prof. Marina", area = AreaMateria.MATEMATICAS), listOf(5))
    }

    /** Trae los grupos del backend y sube los que estén pendientes, en cuanto haya conexión. */
    fun sincronizar() = SyncScheduler.programar(appContext)

    /** BQ 14: el método más usado en salones del mismo tamaño y materia. */
    suspend fun recomendarMetodo(area: AreaMateria, tamano: Int): Recomendacion = agrupacion.recomendar(area, tamano)

    /** El grupo queda pendiente de subir; el SyncWorker lo crea en el backend cuando hay conexión. */
    suspend fun crearGrupo(
        nombre: String,
        docente: String,
        area: AreaMateria,
        ninos: List<Nino>,
        cantidadNinos: Int,
        cantidadDocentes: Int,
        metodo: MetodoAgrupacion
    ) {
        crearEnRoom(
            GrupoEntity(nombre = nombre, docente = docente, area = area, syncStatus = SyncStatus.PENDING),
            ninos.map { it.id }
        )
        sincronizar()
        sesionesAgrupacion.registrar(area, nombre, cantidadNinos, cantidadDocentes, ninos.size)
        agrupacion.registrar(area, cantidadNinos, metodo)
    }

    suspend fun actualizarNombre(id: Int, nuevoNombre: String) = dao.actualizarNombre(id, nuevoNombre)

    suspend fun actualizarDocente(id: Int, nuevoDocente: String) = dao.actualizarDocente(id, nuevoDocente)

    suspend fun quitarNino(id: Int, nino: Nino) = dao.quitarNinoDeGrupo(id, nino.id)

    suspend fun agregarNinos(id: Int, nuevos: List<Nino>) =
        dao.insertarCrossRefs(nuevos.map { GrupoNinoCrossRef(id, it.id) })

    /** Desaparece de la lista al instante y se da de baja en el backend cuando hay conexión. */
    suspend fun eliminarGrupo(id: Int) = gruposRemotos.eliminar(id)

    private suspend fun crearEnRoom(grupo: GrupoEntity, ninoIds: List<Int>) {
        val id = dao.insertarGrupo(grupo).toInt()
        dao.insertarCrossRefs(ninoIds.map { GrupoNinoCrossRef(id, it) })
    }
}
