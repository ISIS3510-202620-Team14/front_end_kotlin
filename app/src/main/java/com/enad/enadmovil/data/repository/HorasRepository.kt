package com.enad.enadmovil.data.repository

import android.content.Context
import com.enad.enadmovil.data.local.AppDatabase
import com.enad.enadmovil.data.local.entity.ReporteHorasEntity
import com.enad.enadmovil.data.local.entity.SyncStatus
import com.enad.enadmovil.data.remote.CloudFunctionsApi
import com.enad.enadmovil.data.sync.SyncScheduler
import com.enad.enadmovil.data.telemetria.InfoDispositivo
import com.enad.enadmovil.domain.model.DiaSesion
import com.enad.enadmovil.domain.model.Horario
import com.enad.enadmovil.domain.model.SugerenciaHoras
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.floor
import kotlin.math.min

/** Horas planeadas (horario de los grupos) y realizadas (lo que el docente guarda), todo en Room. */
class HorasRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dao = AppDatabase.obtener(appContext).contextoDao()
    private val auth = AuthRepository()

    /** Días hábiles de las últimas [semanas] semanas, de lunes a hoy, con lo ya reportado. */
    fun observarDias(semanas: Int = 12, hoy: LocalDate = LocalDate.now()): Flow<List<DiaSesion>> {
        val uid = auth.uidActual() ?: return flowOf(emptyList())
        val desde = hoy.with(DayOfWeek.MONDAY).minusWeeks((semanas - 1).toLong())
        return dao.observarReportes(uid, desde.toString(), hoy.toString()).map { reportes ->
            val grupos = dao.grupos(uid)
            val porFecha = reportes.associateBy { it.fecha }
            generateSequence(desde) { it.plusDays(1) }
                .takeWhile { !it.isAfter(hoy) }
                .filter { it.dayOfWeek.value <= 5 }
                .map { fecha ->
                    val planeadas = grupos.sumOf {
                        Horario.horasDelDia(it.horario, fecha.dayOfWeek) ?: 0.0
                    }
                    DiaSesion(fecha, planeadas, porFecha[fecha.toString()]?.horasRealizadas)
                }.toList()
        }
    }

    suspend fun reporteDelDia(fecha: LocalDate): ReporteHorasEntity? {
        val uid = auth.uidActual() ?: return null
        return dao.reporte(uid, fecha.toString())
    }

    /**
     * Estar en la sede no es dictar clase. Se sugieren las horas planeadas solo si hubo presencia en la sede
     * y asistencia registrada ese día, y nunca más que el tiempo que estuvo en la sede.
     */
    suspend fun sugerencia(fecha: LocalDate, planeadas: Double): SugerenciaHoras? {
        val uid = auth.uidActual() ?: return null
        if (planeadas <= 0) return null
        val presencias = dao.presenciasDelDia(uid, fecha.toString())
        if (presencias.isEmpty()) return null                                  // sin dato de ubicación
        if (dao.asistenciasDelDia(uid, fecha.toString()) == 0) return null     // estuvo, pero sin actividad
        val permanencia = presencias.maxOf { (it.ultimaVez - it.primeraVez) / MS_POR_HORA }
        val horas = floor(min(planeadas, permanencia) * 2) / 2                 // medias horas, sin pasar el tope
        return if (horas > 0) SugerenciaHoras(horas, permanencia) else null
    }

    suspend fun guardar(fecha: LocalDate, planeadas: Double, horas: Double, origen: String, motivo: String?) {
        val uid = auth.uidActual() ?: return
        val presente = dao.presenciasDelDia(uid, fecha.toString()).isNotEmpty()
        dao.guardarReporte(
            ReporteHorasEntity(
                docenteUid = uid,
                fecha = fecha.toString(),
                horasPlaneadas = planeadas,
                horasRealizadas = horas,
                origen = origen,
                motivo = motivo?.trim()?.ifBlank { null },
                presenteEnSede = presente,
                guardadoEn = System.currentTimeMillis()
            )
        )
        SyncScheduler.programar(appContext)
    }

    /** Sube los reportes pendientes. Devuelve false si quedó alguno por reintentar. */
    suspend fun sincronizarPendientes(): Boolean {
        val uid = auth.uidActual() ?: return true
        val pendientes = dao.reportesPendientes(uid)
        if (pendientes.isEmpty()) return true
        val token = runCatching { auth.obtenerToken() }.getOrNull() ?: return false
        val schoolId = runCatching { auth.schoolIdActual() }.getOrNull()
        val version = InfoDispositivo.versionApp(appContext)
        var todoBien = true
        for (r in pendientes) {
            dao.sumarIntentoReporte(uid, r.fecha, r.guardadoEn)
            try {
                CloudFunctionsApi.enviarReporteHoras(token, r, schoolId, InfoDispositivo.PLATAFORMA, version)
                dao.marcarReporte(uid, r.fecha, r.guardadoEn, SyncStatus.SYNCED, null)
            } catch (e: CloudFunctionsApi.ApiException) {
                if (e.esTransitorio) todoBien = false
                else dao.marcarReporte(uid, r.fecha, r.guardadoEn, SyncStatus.FAILED, "${e.status} ${e.code}")
            } catch (e: IOException) {
                todoBien = false
            }
        }
        return todoBien
    }

    private companion object {
        const val MS_POR_HORA = 3_600_000.0
    }
}