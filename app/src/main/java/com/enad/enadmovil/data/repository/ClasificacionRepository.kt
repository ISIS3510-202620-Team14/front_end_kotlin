package com.enad.enadmovil.data.repository

import android.content.Context
import android.util.Log
import com.enad.enadmovil.data.local.ArchivoRespuestas
import com.enad.enadmovil.data.local.EvaluacionPendiente
import com.enad.enadmovil.data.local.EvaluacionesPendientes
import com.enad.enadmovil.data.local.Lectura
import com.enad.enadmovil.data.remote.CloudFunctionsApi
import com.enad.enadmovil.data.sync.EnviarClasificacionWorker
import com.enad.enadmovil.data.sync.EnviarEvaluacionesWorker
import kotlinx.coroutines.tasks.await
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
    private val archivo = ArchivoRespuestas(appContext)

    /** GET /schools: al docente solo le llegan sus instituciones. */
    suspend fun misInstituciones(): List<CloudFunctionsApi.Institucion> =
        CloudFunctionsApi.misInstituciones(token())

    /** GET /students?schoolId=: estudiantes activos de una institución del docente. */
    suspend fun estudiantes(
        schoolId: String
    ): List<CloudFunctionsApi.EstudianteRemoto> =
        CloudFunctionsApi.listarEstudiantes(
            token(),
            schoolId,
            null
        )

    /** GET /students: todos los estudiantes de las instituciones del docente. */
    suspend fun todosLosEstudiantes(): List<CloudFunctionsApi.EstudianteRemoto> =
        CloudFunctionsApi.listarEstudiantes(
            token(),
            null,
            null
        )

    suspend fun misInstitucionesConRespaldo(): Lectura<List<CloudFunctionsApi.Institucion>> =
        conRespaldo("instituciones", { CloudFunctionsApi.misInstitucionesJson(token()) }, CloudFunctionsApi::institucionesDeJson)

    suspend fun estudiantesConRespaldo(schoolId: String): Lectura<List<CloudFunctionsApi.EstudianteRemoto>> =
        conRespaldo("estudiantes_$schoolId", { CloudFunctionsApi.listarEstudiantesJson(token(), schoolId, null) }, CloudFunctionsApi::estudiantesDeJson)

    suspend fun todosLosEstudiantesConRespaldo(): Lectura<List<CloudFunctionsApi.EstudianteRemoto>> =
        conRespaldo("estudiantes", { CloudFunctionsApi.listarEstudiantesJson(token(), null, null) }, CloudFunctionsApi::estudiantesDeJson)

    private suspend fun <T> conRespaldo(nombre: String, pedir: suspend () -> JSONObject, convertir: (JSONObject) -> T): Lectura<T> =
        archivo.conRespaldo(
            uid = auth.uidActual(),
            nombre = nombre,
            esRechazo = { it is CloudFunctionsApi.ApiException && it.status in 400..499 },
            pedir = { pedir().toString() },
            convertir = { convertir(JSONObject(it)) }
        )

    /**
     * POST /students/{id}/evaluations: registra el nivel alcanzado.
     */
    suspend fun registrarEvaluacion(
        estudianteId: String,
        materia: String,
        nivel: String,
        clientId: String,
        // Momento de evaluación del back: inicial (Línea Base), media, final o especifica.
        tipo: String = "especifica"
    ) {
        CloudFunctionsApi.registrarEvaluacion(
            token = token(),
            id = estudianteId,
            subject = materia,
            type = tipo,
            level = nivel,
            clientId = clientId
        )
    }

    /** PATCH /students/{id}: guarda sexo, edad y muestra de la ficha antes de evaluar. */
    suspend fun actualizarFicha(estudianteId: String, sexo: String, edad: Int, muestra: Int) {
        CloudFunctionsApi.actualizarEstudiante(
            token(),
            estudianteId,
            JSONObject().put("gender", sexo).put("age", edad).put("sample", muestra)
        )
    }

    /** PATCH /students/{id} con retired=true: sale de las listas activas, conserva su historial. */
    suspend fun retirar(estudianteId: String) {
        CloudFunctionsApi.actualizarEstudiante(token(), estudianteId, JSONObject().put("retired", true))
    }

    /** GET /students/{id}/progress: lo que muestran el perfil y el historial del estudiante. */
    suspend fun progreso(estudianteId: String): CloudFunctionsApi.ProgresoEstudiante =
        CloudFunctionsApi.progresoEstudiante(token(), estudianteId)

    /** Grupos (nombre, materia) en los que está el estudiante. */
    suspend fun gruposDe(schoolId: String, estudianteId: String): List<Pair<String, String>> =
        CloudFunctionsApi.gruposDeEstudiante(token(), schoolId, estudianteId)

    /** Guarda en el celular una evaluación que no se pudo mandar y programa su envío. */
    fun guardarPendiente(evaluacion: EvaluacionPendiente) {
        val uid = auth.uidActual() ?: return
        EvaluacionesPendientes.agregar(appContext, uid, evaluacion)
        EnviarEvaluacionesWorker.programar(appContext)
    }

    /** Evaluaciones que siguen sin llegar al backend, para mostrarlas en la lista. */
    fun pendientes(materia: String): List<EvaluacionPendiente> {
        val uid = auth.uidActual() ?: return emptyList()
        return EvaluacionesPendientes.todas(appContext, uid).filter { it.materia == materia }
    }

    /**
     * Manda las evaluaciones pendientes en el orden en que se hicieron (ficha y luego nivel).
     * Devuelve false si quedó alguna por reintentar (sin conexión o error del servidor). Una que
     * el backend rechaza (400, 403, 404) sale de la cola: reintentarla no la arregla.
     */
    suspend fun enviarPendientes(): Boolean {
        val uid = auth.uidActual() ?: return true
        for (e in EvaluacionesPendientes.todas(appContext, uid)) {
            try {
                actualizarFicha(e.estudianteId, e.sexo, e.edad, e.muestra)
                CloudFunctionsApi.registrarEvaluacion(token(), e.estudianteId, e.materia, e.tipo, e.nivel, e.clientId, e.fecha)
                EvaluacionesPendientes.quitar(appContext, uid, e.clientId)
            } catch (ex: CloudFunctionsApi.ApiException) {
                if (ex.esTransitorio) return false
                Log.w("ClasificacionRepository", "El backend rechazó la evaluación ${e.clientId}: ${ex.status} ${ex.code}")
                EvaluacionesPendientes.quitar(appContext, uid, e.clientId)
            } catch (ex: Exception) {
                return false // sin conexión: se reintenta con lo que falta
            }
        }
        return true
    }

    /** Deja la sesión en cola: WorkManager la manda cuando haya internet, sin duplicarla. */
    fun enviarSesion(sesion: JSONObject) {
        EnviarClasificacionWorker.programar(
            appContext,
            sesion
        )
    }

    private suspend fun token(): String =
        auth.obtenerToken()
            ?: throw CloudFunctionsApi.ApiException(
                "unauthenticated",
                "Tu sesión expiró. Vuelve a iniciar sesión.",
                401
            )
}