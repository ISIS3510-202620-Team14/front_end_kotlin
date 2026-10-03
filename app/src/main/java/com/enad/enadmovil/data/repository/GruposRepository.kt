package com.enad.enadmovil.data.repository

import android.content.Context
import com.enad.enadmovil.data.local.EnadDatabase
import com.enad.enadmovil.data.local.GrupoEntity
import com.enad.enadmovil.data.local.GrupoNinoCrossRef
import com.enad.enadmovil.data.local.NinoEntity
import com.enad.enadmovil.data.mapper.aGrupo
import com.enad.enadmovil.domain.model.AreaMateria
import com.enad.enadmovil.domain.model.Grupo
import com.enad.enadmovil.domain.model.Nino
import com.enad.enadmovil.feature.grupos.ninosDeEjemplo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository: grupos de agrupación guardados en Room. El ViewModel solo ve modelos de dominio
 * (Grupo, Nino); las entidades y el DAO se quedan en esta capa. Al crear un grupo también
 * registra la sesión de agrupación (BQ tipo 1).
 */
class GruposRepository(context: Context) {
    private val dao = EnadDatabase.obtener(context.applicationContext).gruposDao()
    private val sesionesAgrupacion = SesionAgrupacionRepository(context)

    fun observarGrupos(area: AreaMateria): Flow<List<Grupo>> =
        dao.observarGrupos().map { grupos -> grupos.filter { it.grupo.area == area }.map { it.aGrupo() } }

    /** La primera vez que se abre la app, deja tres grupos de ejemplo para que la pantalla no esté vacía. */
    suspend fun sembrarSiVacio() {
        if (dao.contarGrupos() > 0) return
        dao.insertarNinos(ninosDeEjemplo.map { NinoEntity(it.id, it.nombre, it.nivel, it.grado) })
        crearEnRoom("Grupo Abejitas", "Yo", AreaMateria.MATEMATICAS, listOf(2, 6))
        crearEnRoom("Grupo Colibríes", "Prof. Nelson", AreaMateria.MATEMATICAS, listOf(1, 7))
        crearEnRoom("Grupo Tortugas", "Prof. Marina", AreaMateria.MATEMATICAS, listOf(5))
    }

    suspend fun crearGrupo(
        nombre: String,
        docente: String,
        area: AreaMateria,
        ninos: List<Nino>,
        cantidadNinos: Int,
        cantidadDocentes: Int
    ) {
        crearEnRoom(nombre, docente, area, ninos.map { it.id })
        sesionesAgrupacion.registrar(area, nombre, cantidadNinos, cantidadDocentes, ninos.size)
    }

    suspend fun actualizarNombre(id: Int, nuevoNombre: String) = dao.actualizarNombre(id, nuevoNombre)

    suspend fun actualizarDocente(id: Int, nuevoDocente: String) = dao.actualizarDocente(id, nuevoDocente)

    suspend fun quitarNino(id: Int, nino: Nino) = dao.quitarNinoDeGrupo(id, nino.id)

    suspend fun agregarNinos(id: Int, nuevos: List<Nino>) =
        dao.insertarCrossRefs(nuevos.map { GrupoNinoCrossRef(id, it.id) })

    suspend fun eliminarGrupo(id: Int) = dao.eliminarGrupo(id)

    private suspend fun crearEnRoom(nombre: String, docente: String, area: AreaMateria, ninoIds: List<Int>) {
        val id = dao.insertarGrupo(GrupoEntity(nombre = nombre, docente = docente, area = area)).toInt()
        dao.insertarCrossRefs(ninoIds.map { GrupoNinoCrossRef(id, it) })
    }
}
