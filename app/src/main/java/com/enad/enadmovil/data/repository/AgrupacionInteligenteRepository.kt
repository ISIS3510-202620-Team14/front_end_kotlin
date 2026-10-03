package com.enad.enadmovil.data.repository

import android.content.Context
import com.enad.enadmovil.data.local.AppDatabase
import com.enad.enadmovil.data.local.entity.EventoAgrupacionEntity
import com.enad.enadmovil.data.local.entity.SyncStatus
import com.enad.enadmovil.data.remote.CloudFunctionsApi
import com.enad.enadmovil.data.sync.SyncScheduler
import com.enad.enadmovil.domain.model.AreaMateria
import com.enad.enadmovil.domain.model.EventoAgrupacion
import com.enad.enadmovil.domain.model.MetodoAgrupacion
import com.enad.enadmovil.domain.model.Recomendacion
import com.enad.enadmovil.domain.model.RecomendadorAgrupacion
import com.enad.enadmovil.domain.model.clave
import java.io.IOException
import java.util.UUID

/** BQ 14: guarda cómo armó el docente cada grupo y recomienda el método para el siguiente. */
class AgrupacionInteligenteRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dao = AppDatabase.obtener(appContext).eventoAgrupacionDao()
    private val auth = AuthRepository()

    /** Guarda el evento del grupo recién creado y programa su subida. Funciona sin conexión. */
    suspend fun registrar(area: AreaMateria, tamano: Int, metodo: MetodoAgrupacion) {
        val uid = auth.uidActual() ?: return
        dao.insertar(
            EventoAgrupacionEntity(
                id = UUID.randomUUID().toString(),
                docenteUid = uid,
                subject = area.clave,
                classSize = tamano,
                method = metodo.clave,
                creadoEn = System.currentTimeMillis()
            )
        )
        SyncScheduler.programar(appContext)
    }

    /**
     * Mezcla lo que registró el resto de docentes en el backend con los eventos propios que todavía no subieron.
     * Sin conexión usa solo los eventos guardados en el dispositivo.
     */
    suspend fun recomendar(area: AreaMateria, tamano: Int): Recomendacion {
        val materia = area.clave
        val locales = auth.uidActual()?.let { dao.eventos(it, materia) }.orEmpty()
        val remotos = runCatching {
            val token = auth.obtenerToken() ?: error("sin sesión")
            CloudFunctionsApi.eventosAgrupacion(token, materia)
        }.getOrNull()

        val eventos = if (remotos != null) {
            // Los propios que ya subieron vienen dentro de los remotos: no se cuentan dos veces
            remotos.mapNotNull { r -> MetodoAgrupacion.deClave(r.metodo)?.let { EventoAgrupacion(r.materia, r.tamano, it) } } +
                locales.filter { it.syncStatus != SyncStatus.SYNCED }.mapNotNull { it.aEvento() }
        } else {
            locales.mapNotNull { it.aEvento() }
        }
        return RecomendadorAgrupacion.recomendar(eventos, materia, tamano)
    }

    suspend fun sincronizarPendientes(): Boolean {
        val uid = auth.uidActual() ?: return true
        val pendientes = dao.pendientes(uid)
        if (pendientes.isEmpty()) return true
        val token = runCatching { auth.obtenerToken() }.getOrNull() ?: return false
        var todoBien = true
        for (e in pendientes) {
            dao.sumarIntento(e.id)
            try {
                CloudFunctionsApi.enviarEventoAgrupacion(token, e.subject, e.classSize, e.method, uid, e.creadoEn)
                dao.marcar(e.id, SyncStatus.SYNCED, null)
            } catch (ex: CloudFunctionsApi.ApiException) {
                if (ex.esTransitorio) todoBien = false
                else dao.marcar(e.id, SyncStatus.FAILED, "${ex.status} ${ex.code}")
            } catch (ex: IOException) {
                todoBien = false
            }
        }
        return todoBien
    }

    private fun EventoAgrupacionEntity.aEvento(): EventoAgrupacion? =
        MetodoAgrupacion.deClave(method)?.let { EventoAgrupacion(subject, classSize, it) }
}
