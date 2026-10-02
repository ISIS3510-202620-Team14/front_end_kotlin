package com.enad.enadmovil.data.repository

import android.content.Context
import com.enad.enadmovil.data.local.AppDatabase
import com.enad.enadmovil.data.local.entity.SesionAgrupacionEntity
import com.enad.enadmovil.data.local.entity.SyncStatus
import com.enad.enadmovil.data.remote.CloudFunctionsApi
import com.enad.enadmovil.data.sync.SyncScheduler
import com.enad.enadmovil.data.telemetria.InfoDispositivo
import com.enad.enadmovil.domain.model.AreaMateria
import java.io.IOException
import java.util.UUID

class SesionAgrupacionRepository(context: Context) {
    private val appContext = context.applicationContext
    private val sesiones = AppDatabase.obtener(appContext).sesionAgrupacionDao()
    private val auth = AuthRepository()

    suspend fun registrar(materia: AreaMateria, nombreGrupo: String, ninosContados: Int, docentesContados: Int, ninosAsignados: Int) {
        val creadaEn = System.currentTimeMillis()
        val conectividad = InfoDispositivo.conectividad(appContext)
        val uid = auth.uidActual() ?: return

        sesiones.insertar(SesionAgrupacionEntity(
            id = UUID.randomUUID().toString(),
            docenteUid = uid,
            schoolId = auth.schoolIdActual(),
            subject = if (materia == AreaMateria.MATEMATICAS) "matematicas" else "lectura",
            groupName = nombreGrupo.trim(),
            studentsCounted = ninosContados,
            teachersCounted = docentesContados,
            studentsAssigned = ninosAsignados,
            creadaEn = creadaEn,
            connectivity = conectividad,
            platform = InfoDispositivo.PLATAFORMA,
            appVersion = InfoDispositivo.versionApp(appContext),
            syncStatus = SyncStatus.PENDING
        ))
        SyncScheduler.programar(appContext)
    }

    suspend fun sincronizarPendientes(): Boolean {
        val uid = auth.uidActual() ?: return true
        val pendientes = sesiones.pendientes(uid)
        if(pendientes.isEmpty()) return true
        val token = runCatching { auth.obtenerToken() }.getOrNull() ?: return false
        var todoBien = true
        for (s in pendientes) {
            sesiones.sumarIntento(s.id)
            try {
                CloudFunctionsApi.enviarSesionAgrupacion(token, s, intento = s.intentos + 1)
                sesiones.marcar(s.id, SyncStatus.SYNCED, System.currentTimeMillis(), null)
            } catch(e: CloudFunctionsApi.ApiException) {
                if (e.esTransitorio) todoBien = false
                else sesiones.marcar(s.id, SyncStatus.FAILED, null, "${e.status} ${e.code}")
            } catch (e: IOException) {
                todoBien = false
            }
        }
        return todoBien
    }
}