package com.enad.enadmovil.data.repository

import com.enad.enadmovil.data.remote.CloudFunctionsApi

/**
 * Repository de los reportes del admin (pestaña Reportes). Los endpoints exigen rol admin:
 * si el usuario no lo es, el backend responde 403 y la ApiException llega al ViewModel.
 */
class ReportesRepository(
    private val auth: AuthRepository = AuthRepository()
) {
    /** BQ tipo 1: sesiones de agrupación que no se sincronizaron en 24 h. */
    suspend fun sincronizacionAgrupaciones(): CloudFunctionsApi.ReporteSincronizacion =
        CloudFunctionsApi.reporteSesionesAgrupacion(token())

    /** BQ #9: minutos por cada 25 estudiantes, por institución y docente. null = las dos materias. */
    suspend fun tiempoClasificacion(materia: String?): List<CloudFunctionsApi.InstitucionClasificacion> =
        CloudFunctionsApi.reporteClasificacion(token(), materia)

    /** BQ #6: docentes que abrieron la app más de una vez en la semana (lunes yyyy-MM-dd; null = la actual). */
    suspend fun aperturas(semana: String?): CloudFunctionsApi.ReporteAperturas =
        CloudFunctionsApi.reporteAperturas(token(), semana)

    /** Horas planeadas y realizadas por semana, y cuántos días se aceptó la sugerencia. */
    suspend fun horas(): List<CloudFunctionsApi.SemanaHoras> =
        CloudFunctionsApi.reporteHoras(token())

    /** BQ #11: actividades más elegidas. */
    suspend fun topActividades(): CloudFunctionsApi.TopActividades =
        CloudFunctionsApi.topActividades(token())

    suspend fun instituciones(busqueda: String = ""): List<CloudFunctionsApi.EscuelaAdmin> =
        CloudFunctionsApi.institucionesAdmin(token(), busqueda)

    suspend fun docentes(schoolId: String): List<CloudFunctionsApi.DocenteAdmin> =
        CloudFunctionsApi.docentesDeEscuela(token(), schoolId)

    private suspend fun token(): String =
        auth.obtenerToken()
            ?: throw CloudFunctionsApi.ApiException("unauthenticated", "Inicia sesión de nuevo para ver el reporte.", 401)
}
