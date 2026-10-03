package com.enad.enadmovil.data.repository

import android.content.Context
import com.enad.enadmovil.data.remote.CloudFunctionsApi
import com.enad.enadmovil.data.sync.EnviarClasificacionWorker
import org.json.JSONObject

/**
 * Repository de la pantalla de Clasificación: instituciones del docente, sus estudiantes y
 * las sesiones cronometradas de la BQ 9. El ViewModel no sabe de tokens, URLs ni WorkManager.
 */
class ClasificacionRepository(
    context: Context,
    private val auth: AuthRepository = AuthRepository()
) {
    private val appContext = context.applicationContext

    /** GET /schools: al docente solo le llegan sus instituciones. */
    suspend fun misInstituciones(): List<CloudFunctionsApi.Institucion> =
        CloudFunctionsApi.misInstituciones(token())

    /** GET /students?schoolId= : estudiantes activos de una institución del docente. */
    suspend fun estudiantes(schoolId: String): List<CloudFunctionsApi.EstudianteRemoto> =
        CloudFunctionsApi.listarEstudiantes(token(), schoolId, null)

    /** Deja la sesión en cola: WorkManager la manda cuando haya internet, sin duplicarla. */
    fun enviarSesion(sesion: JSONObject) {
        EnviarClasificacionWorker.programar(appContext, sesion)
    }

    private suspend fun token(): String =
        auth.obtenerToken()
            ?: throw CloudFunctionsApi.ApiException("unauthenticated", "Tu sesión expiró. Vuelve a iniciar sesión.", 401)
}
