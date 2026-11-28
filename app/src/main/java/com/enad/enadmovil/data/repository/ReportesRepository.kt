package com.enad.enadmovil.data.repository

import com.enad.enadmovil.data.remote.CloudFunctionsApi
import com.enad.enadmovil.data.cache.CacheMemoria
import com.enad.enadmovil.data.local.ArchivoRespuestas
import com.enad.enadmovil.data.local.Lectura
import com.enad.enadmovil.domain.model.DocentesEnRiesgo
import com.enad.enadmovil.domain.model.RiesgoAbandono
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/**
 * Repository de los reportes del admin (pestaña Reportes). Los endpoints exigen rol admin:
 * si el usuario no lo es, el backend responde 403 y la ApiException llega al ViewModel.
 */
class ReportesRepository(
    private val auth: AuthRepository = AuthRepository(),
    private val archivo: ArchivoRespuestas? = null
) {

    companion object {
        private const val MAX_ENTRADAS = 32
        private const val tiempoDeVidaMs = 5 * 60 * 1000L

        private val cache = CacheMemoria(MAX_ENTRADAS, tiempoDeVidaMs, "reportes")

        fun limpiarCache() = cache.limpiar()
    }
    /** BQ tipo 1: sesiones de agrupación que no se sincronizaron en 24 h. */
    suspend fun sincronizacionAgrupaciones(forzar: Boolean = false): CloudFunctionsApi.ReporteSincronizacion =
        cache.obtenerOCargar("sincronización", forzar) { CloudFunctionsApi.reporteSesionesAgrupacion(token()) }

    /** BQ #9: minutos por cada 25 estudiantes, por institución y docente. null = las dos materias. */
    suspend fun tiempoClasificacion(materia: String?, forzar: Boolean = false): List<CloudFunctionsApi.InstitucionClasificacion> =
        cache.obtenerOCargar("clasificacion:${materia ?: "todas"}", forzar) { CloudFunctionsApi.reporteClasificacion(token(), materia) }

    /** BQ #6: docentes que abrieron la app más de una vez en la semana (lunes yyyy-MM-dd; null = la actual). */
    suspend fun aperturas(semana: String?, forzar: Boolean = false): CloudFunctionsApi.ReporteAperturas =
        cache.obtenerOCargar(clave = "aperturas:${semana ?: "actual"}",
            forzar = forzar, vence = { it.provisional }) { CloudFunctionsApi.reporteAperturas(token(), semana) }

    /** Horas planeadas y realizadas por semana, y cuántos días se aceptó la sugerencia. */
    suspend fun horas(forzar: Boolean = false): List<CloudFunctionsApi.SemanaHoras> =
        cache.obtenerOCargar("horas", forzar) { CloudFunctionsApi.reporteHoras(token()) }

    /** BQ #11: actividades más elegidas. */
    suspend fun topActividades(forzar: Boolean = false): CloudFunctionsApi.TopActividades =
        cache.obtenerOCargar("actividades", forzar) { CloudFunctionsApi.topActividades(token()) }

    /** BQ #12: evaluaciones por materia y por grado; la materia menos usada es la que conviene reforzar. */
    suspend fun evaluacionesPorGrado(forzar: Boolean = false): Lectura<CloudFunctionsApi.EvaluacionesPorGrado> {
        val lectura = cache.obtenerOCargar("evaluaciones", forzar) { leerEvaluacionesPorGrado() }
        if (lectura.guardadoEn != null) cache.invalidar("evaluaciones")
        return lectura
    }

    private suspend fun leerEvaluacionesPorGrado(): Lectura<CloudFunctionsApi.EvaluacionesPorGrado> {
        val pedir: suspend () -> JSONObject = { CloudFunctionsApi.evaluacionesPorGradoJson(token()) }
        val almacen = archivo ?: return Lectura(CloudFunctionsApi.evaluacionesDeJson(pedir()))
        return almacen.conRespaldo(
            uid = auth.uidActual(),
            nombre = "evaluaciones",
            esRechazo = { it is CloudFunctionsApi.ApiException && it.status in 400..499 },
            pedir = { pedir().toString() },
            convertir = { CloudFunctionsApi.evaluacionesDeJson(JSONObject(it)) }
        )
    }

    /** BQ #10: calidad del escaneo según las condiciones de la foto (últimas 8 semanas). */
    suspend fun calidadEscaneo(forzar: Boolean = false): CloudFunctionsApi.CalidadEscaneo =
        cache.obtenerOCargar("escaneo", forzar) { CloudFunctionsApi.calidadEscaneo(token()) }

    /** BQ #11: qué corrige el docente en la revisión del escaneo (últimas 8 semanas). */
    suspend fun revisionEscaneo(forzar: Boolean = false): CloudFunctionsApi.RevisionEscaneo =
        cache.obtenerOCargar("revision-escaneo", forzar) { CloudFunctionsApi.revisionEscaneo(token()) }

    /** BQ tipo 3: en qué paso de cada guía abandonan y qué pasos se saltan (últimas 8 semanas). */
    suspend fun pasosGuias(forzar: Boolean = false): CloudFunctionsApi.PasosGuias =
        cache.obtenerOCargar("guias", forzar) { CloudFunctionsApi.pasosGuias(token()) }

    /** BQ Sprint 3 tipo 1: marcas de asistencia que otro celular ya había registrado (semana lunes yyyy-MM-dd). */
    suspend fun conflictosAsistencia(semana: String?, forzar: Boolean = false): CloudFunctionsApi.ReporteConflictos =
        cache.obtenerOCargar(clave = "conflictos:${semana ?: "actual"}",
            forzar = forzar, vence = { it.provisional }) { CloudFunctionsApi.reporteConflictos(token(), semana) }

    /** BQ tipo 1: sesiones que terminan en un fallo, por pantalla, versión, modelo y Android. Sin red usa la copia guardada. */
    suspend fun fallos(forzar: Boolean = false): Lectura<CloudFunctionsApi.ReporteFallos> {
        val lectura = cache.obtenerOCargar("fallos", forzar) { leerFallos() }
        if (lectura.guardadoEn != null) cache.invalidar("fallos")
        return lectura
    }

    private suspend fun leerFallos(): Lectura<CloudFunctionsApi.ReporteFallos> {
        val pedir: suspend () -> JSONObject = { CloudFunctionsApi.reporteFallosJson(token()) }
        val almacen = archivo ?: return Lectura(CloudFunctionsApi.fallosDeJson(pedir()))
        return almacen.conRespaldo(
            uid = auth.uidActual(),
            nombre = "fallos",
            esRechazo = { it is CloudFunctionsApi.ApiException && it.status in 400..499 },
            pedir = { pedir().toString() },
            convertir = { CloudFunctionsApi.fallosDeJson(JSONObject(it)) }
        )
    }

    suspend fun docentesEnRiesgo(forzar: Boolean = false): Lectura<DocentesEnRiesgo> {
        val lunes = LocalDate.now(ZoneId.of("America/Bogota")).with(DayOfWeek.MONDAY)
        val semanas = (RiesgoAbandono.SEMANAS_BASE + 1 downTo 1).map { lunes.minusWeeks(it.toLong()).toString() }
        val clave = "riesgo:${semanas.last()}"
        val lectura = cache.obtenerOCargar(clave, forzar) { leerDocentesEnRiesgo(semanas) }
        if (lectura.guardadoEn != null) cache.invalidar(clave)
        return lectura
    }

    private suspend fun leerDocentesEnRiesgo(semanas: List<String>): Lectura<DocentesEnRiesgo> {
        val almacen = archivo ?: return Lectura(calcularDocentesEnRiesgo(semanas))
        return almacen.conRespaldo(
            uid = auth.uidActual(),
            nombre = "riesgo",
            esRechazo = { it is CloudFunctionsApi.ApiException && it.status in 400..499 },
            pedir = { calcularDocentesEnRiesgo(semanas).aJson() },
            convertir = DocentesEnRiesgo::deJson
        )
    }

    private suspend fun calcularDocentesEnRiesgo(semanas: List<String>): DocentesEnRiesgo = coroutineScope {
        val token = token()
        val respuestas = semanas.map { semana -> async { CloudFunctionsApi.aperturasPorDocente(token, semana) } }.awaitAll()
        withContext(Dispatchers.Default) { DocentesEnRiesgo.calcular(semanas.last(), respuestas) }
    }

    suspend fun instituciones(busqueda: String = "", forzar: Boolean = false): List<CloudFunctionsApi.EscuelaAdmin> =
        cache.obtenerOCargar("instituciones:$busqueda", forzar) { CloudFunctionsApi.institucionesAdmin(token(), busqueda) }

    suspend fun docentes(schoolId: String, forzar: Boolean = false): List<CloudFunctionsApi.DocenteAdmin> =
        cache.obtenerOCargar("docentes:$schoolId", forzar) { CloudFunctionsApi.docentesDeEscuela(token(), schoolId) }

    private suspend fun token(): String =
        auth.obtenerToken()
            ?: throw CloudFunctionsApi.ApiException("unauthenticated", "Inicia sesión de nuevo para ver el reporte.", 401)
}
