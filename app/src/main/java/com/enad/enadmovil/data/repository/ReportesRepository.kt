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

    private suspend fun token(): String =
        auth.obtenerToken()
            ?: throw CloudFunctionsApi.ApiException("unauthenticated", "Inicia sesión de nuevo para ver el reporte.", 401)
}
