package com.enad.enadmovil.data.repository

import android.content.Context
import com.enad.enadmovil.data.local.EnadDatabase
import com.enad.enadmovil.data.local.GrupoEntity
import com.enad.enadmovil.data.local.entity.SyncStatus
import com.enad.enadmovil.data.remote.CloudFunctionsApi
import com.enad.enadmovil.data.sync.SyncScheduler
import com.enad.enadmovil.domain.model.AreaMateria
import com.enad.enadmovil.domain.model.clave
import java.io.IOException

/**
 * Los grupos viven primero en el dispositivo y se reflejan en el backend cuando hay conexión:
 * los nuevos se crean allá, los eliminados se dan de baja y los que el docente ya tiene en el backend se traen.
 */
class GruposRemotosRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dao = EnadDatabase.obtener(appContext).gruposDao()
    private val auth = AuthRepository()

    /** El grupo desaparece de la lista al instante y se da de baja en el backend en cuanto hay conexión. */
    suspend fun eliminar(id: Int) {
        dao.marcarEliminado(id)
        SyncScheduler.programar(appContext)
    }

    /** Devuelve false si quedó algo por reintentar (sin conexión o error del servidor). */
    suspend fun sincronizarPendientes(): Boolean {
        val uid = auth.uidActual() ?: return true
        val token = runCatching { auth.obtenerToken() }.getOrNull() ?: return false
        val schoolId = runCatching { auth.schoolIdActual() }.getOrNull()
        var todoBien = true

        for (grupo in dao.pendientesDeCrear()) {
            try {
                val remoteId = CloudFunctionsApi.crearGrupoRemoto(token, grupo.nombre, grupo.area.clave, schoolId)
                dao.marcarSubido(grupo.id, remoteId)
            } catch (e: CloudFunctionsApi.ApiException) {
                if (e.esTransitorio) todoBien = false else dao.marcarFallido(grupo.id)
            } catch (e: IOException) {
                todoBien = false
            }
        }

        for (grupo in dao.pendientesDeEliminar()) {
            val remoteId = grupo.remoteId
            if (remoteId == null) {
                dao.eliminarGrupo(grupo.id)
                continue
            }
            try {
                CloudFunctionsApi.eliminarGrupoRemoto(token, remoteId)
                dao.eliminarGrupo(grupo.id)
            } catch (e: CloudFunctionsApi.ApiException) {
                when {
                    e.status == 404 -> dao.eliminarGrupo(grupo.id) // ya estaba dado de baja
                    e.esTransitorio -> todoBien = false
                }
            } catch (e: IOException) {
                todoBien = false
            }
        }

        try {
            val conocidos = dao.idsRemotos().toSet()
            CloudFunctionsApi.listarGruposDelDocente(token, uid)
                .filter { it.id !in conocidos }
                .forEach { remoto ->
                    val area = AreaMateria.values().firstOrNull { it.clave == remoto.materia } ?: return@forEach
                    dao.insertarGrupo(
                        GrupoEntity(nombre = remoto.nombre, docente = "Yo", area = area, remoteId = remoto.id, syncStatus = SyncStatus.SYNCED)
                    )
                }
        } catch (e: CloudFunctionsApi.ApiException) {
            if (e.esTransitorio) todoBien = false
        } catch (e: IOException) {
            todoBien = false
        }
        return todoBien
    }
}
