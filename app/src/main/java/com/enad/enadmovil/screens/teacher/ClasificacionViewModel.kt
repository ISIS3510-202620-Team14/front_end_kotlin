package com.enad.enadmovil.ui.screens.teacher

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enad.enadmovil.data.local.EvaluacionPendiente
import com.enad.enadmovil.data.remote.CloudFunctionsApi
import com.enad.enadmovil.data.remote.CloudFunctionsApi.Institucion
import com.enad.enadmovil.data.repository.ClasificacionRepository
import com.enad.enadmovil.data.telemetria.InfoDispositivo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.Instant
import java.util.UUID

data class ClasificacionUiState(
    val cargando: Boolean = true,
    val mensaje: String? = null,
    val instituciones: List<Institucion> = emptyList(),
    val institucionId: String? = null,
    val grado: Int? = null,   // null = "Todos"
    // Momento de evaluación (clave del back). Sin momento no se puede registrar.
    val momento: String? = null,
    val guardadoEn: Long? = null,
    val estudiantes: List<EstudianteClasificacion> = emptyList()   // todos los de la institución elegida
) {
    val visibles: List<EstudianteClasificacion>
        get() = if (grado == null) estudiantes else estudiantes.filter { it.grado == grado }
}

/** Nivel especial que saca al estudiante del programa: no cuenta como clasificación para la BQ 9. */
internal const val NIVEL_RETIRADO = "Retirado"

/** Lo que ve el docente cuando la evaluación se guardó en el celular por falta de conexión. */
internal const val MENSAJE_SIN_CONEXION =
    "Sin conexión: la evaluación quedó guardada en el celular y se envía cuando vuelva la red."

/**
 * Un error que se arregla solo con volver a intentar más tarde (sin red, servidor caído).
 * Un rechazo del backend (datos inválidos, sin permiso, sesión vencida) no se guarda para después.
 */
internal fun esFallaDeConexion(e: Throwable): Boolean =
    e !is CloudFunctionsApi.ApiException || e.status >= 500 || e.status == 408 || e.status == 429

/**
 * Pone encima de la lista (que puede venir del respaldo guardado) las evaluaciones que todavía
 * no llegan al backend, para que el docente vea el nivel que acaba de poner aunque no haya red.
 * Si un estudiante tiene varias, gana la más reciente.
 */
internal fun aplicarPendientes(
    estudiantes: List<EstudianteClasificacion>,
    pendientes: List<EvaluacionPendiente>
): List<EstudianteClasificacion> {
    val ultima = pendientes.groupBy { it.estudianteId }.mapValues { (_, lista) -> lista.maxBy { it.fecha } }
    return estudiantes.map { e ->
        val p = ultima[e.id] ?: return@map e
        e.copy(sexo = p.sexo, edad = p.edad.toString(), muestra = p.muestra, nivelActual = p.etiqueta, ultimaEvaluacion = p.fecha)
    }
}

/** Momentos de evaluación del back con su nombre en pantalla (igual que en Flutter). */
internal val MOMENTOS_EVALUACION = linkedMapOf(
    "inicial" to "Línea Base",
    "media" to "Línea Media",
    "final" to "Línea Final",
    "especifica" to "Evaluación específica",
)

/**
 * Carga las instituciones del docente con sesión abierta (GET /schools) y los estudiantes
 * de la que elija (GET /students?schoolId=). El filtro por curso se hace en memoria para
 * que cambiar de chip no vuelva a pedir la lista.
 *
 * BQ 9: también cronometra cuánto tarda el docente en clasificar (ver [SesionClasificacion]).
 */
class ClasificacionViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ClasificacionRepository(app)
    private var materiaClave = ""
    private var cargaEstudiantes: Job? = null

    // Lo que el docente editó en la tarjeta (sexo, edad, nivel). Sobrevive a los cambios de filtro.
    private val ediciones = mutableMapOf<String, EstudianteClasificacion>()

    private var sesion: SesionClasificacion? = null
    private var enPrimerPlano = false

    private val _uiState = MutableStateFlow(ClasificacionUiState())
    val uiState: StateFlow<ClasificacionUiState> = _uiState.asStateFlow()

    fun iniciar(materia: MateriaConfig) {
        if (materiaClave.isNotEmpty()) return
        materiaClave = materia.clave
        cargarInstituciones()
    }

    fun reintentar() {
        val actual = _uiState.value.institucionId
        if (actual == null) cargarInstituciones() else cargarEstudiantes(actual)
    }

    fun seleccionarInstitucion(id: String) {
        if (id == _uiState.value.institucionId) return
        cerrarSesion()   // cada institución es una sesión aparte en el reporte
        _uiState.update { it.copy(institucionId = id, estudiantes = emptyList()) }
        cargarEstudiantes(id)
    }

    fun seleccionarGrado(grado: Int?) {
        _uiState.update { it.copy(grado = grado) }
    }

    fun seleccionarMomento(momento: String) {
        _uiState.update { it.copy(momento = momento) }
    }

    fun actualizar(id: String, cambio: (EstudianteClasificacion) -> EstudianteClasificacion) {
        _uiState.update { estado ->
            estado.copy(estudiantes = estado.estudiantes.map { e ->
                if (e.id == id) cambio(e).also { ediciones[id] = it } else e
            })
        }
    }

    fun clasificar(id: String, nivel: String) {
        val estudiante = _uiState.value.estudiantes.find { it.id == id } ?: return
        val edad = estudiante.edad.toIntOrNull() ?: return
        evaluar(id, nivel, estudiante.sexo ?: return, edad, estudiante.muestra ?: return) {}
    }

    /**
     * Guarda la ficha (sexo, edad, muestra) y registra el nivel en el momento elegido.
     * Sin conexión, la evaluación queda en el celular y se manda cuando vuelva la red.
     * [alTerminar] recibe el mensaje para el snackbar.
     */
    fun evaluar(id: String, nivel: String, sexo: String, edad: Int, muestra: Int, alTerminar: (String) -> Unit) {
        val momento = _uiState.value.momento ?: return alTerminar("Selecciona el momento de evaluación.")
        val nivelBackend = nivelBackend(nivel) ?: return

        // Mismo clientId y fecha si termina guardada para después: el reintento no la duplica
        val clientId = UUID.randomUUID().toString()
        val fecha = Instant.now().toString()
        val registrada = {
            actualizar(id) {
                it.copy(sexo = sexo, edad = edad.toString(), muestra = muestra, nivelActual = nivel, ultimaEvaluacion = fecha)
            }
            sesion?.registrarClasificacion(id)
            guardarAvance()
        }

        viewModelScope.launch {
            try {
                repo.actualizarFicha(id, sexo, edad, muestra)
                repo.registrarEvaluacion(
                    estudianteId = id,
                    materia = materiaClave,
                    nivel = nivelBackend,
                    clientId = clientId,
                    tipo = momento
                )
                registrada()
                alTerminar("Evaluación guardada.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (esFallaDeConexion(e)) {
                    repo.guardarPendiente(
                        EvaluacionPendiente(clientId, id, materiaClave, nivelBackend, nivel, momento, fecha, sexo, edad, muestra)
                    )
                    registrada()
                    alTerminar(MENSAJE_SIN_CONEXION)
                } else {
                    // Un rechazo del backend no se aplica: la pantalla sigue mostrando lo guardado
                    android.util.Log.e("ClasificacionViewModel", "No se pudo guardar la evaluación", e)
                    alTerminar(mensajeGuardado(e))
                }
            }
        }
    }

    /** Retira al estudiante de las listas activas de todas las áreas; su historial se conserva. */
    fun retirar(id: String, alTerminar: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repo.retirar(id)
                actualizar(id) { it.copy(nivelActual = NIVEL_RETIRADO) }
            } catch (e: Exception) {
                alTerminar(mensajeGuardado(e))
            }
        }
    }
    private fun nivelBackend(nivel: String): String? {
        return when (materiaClave) {
            "matematicas" -> when (nivel) {
                "Principiante" -> "principiante"
                "1 dígito" -> "un_digito"
                "2 dígitos" -> "dos_digitos"
                "Resta" -> "resta"
                "División" -> "division"
                "Problema escrito" -> "problema_escrito"
                else -> null
            }

            "lectura" -> when (nivel) {
                "Principiante" -> "principiante"
                "Letra" -> "letra"
                "Palabra" -> "palabra"
                "Párrafo" -> "parrafo"
                "Cuento" -> "cuento"
                "Comprensión" -> "comprension"
                else -> null
            }

            else -> null
        }
    }

    // La pantalla avisa cuando queda visible u oculta: el tiempo en segundo plano no se cuenta.
    fun alReanudar() {
        enPrimerPlano = true
        sesion?.reanudar()
    }

    fun alPausar() {
        enPrimerPlano = false
        sesion?.pausar()
        guardarAvance()
    }

    override fun onCleared() {
        cerrarSesion()
        super.onCleared()
    }

    private fun abrirSesion(schoolId: String, estudiantes: Int) {
        cerrarSesion()
        if (estudiantes == 0) return
        sesion = SesionClasificacion(schoolId, estudiantes).also { if (enPrimerPlano) it.reanudar() }
    }

    private fun cerrarSesion() {
        guardarAvance()
        sesion = null
    }

    /**
     * Si el docente clasificó al menos un estudiante, deja en cola la sesión con lo que lleva.
     * Se llama tras cada clasificación y al pausar, así el reporte del admin la ve mientras avanza
     * y no se pierde si la app se cierra de golpe (WorkManager la guarda en disco).
     */
    private fun guardarAvance() {
        val actual = sesion ?: return
        val cuerpo = actual.aJson(materiaClave, InfoDispositivo.versionApp(getApplication())) ?: return
        repo.enviarSesion(cuerpo)
    }

    private fun cargarInstituciones() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, mensaje = null) }
            try {
                val instituciones = repo.misInstitucionesConRespaldo().datos
                val primera = instituciones.firstOrNull()?.id
                _uiState.update {
                    it.copy(
                        instituciones = instituciones,
                        institucionId = primera,
                        cargando = primera != null,
                        mensaje = if (primera == null) "No tienes instituciones asignadas." else null
                    )
                }
                if (primera != null) cargarEstudiantes(primera)
            } catch (e: Exception) {
                _uiState.update { it.copy(cargando = false, mensaje = mensajeDe(e)) }
            }
        }
    }

    private fun cargarEstudiantes(schoolId: String) {
        cargaEstudiantes?.cancel()
        cargaEstudiantes = viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, mensaje = null) }
            try {
                val lectura = repo.estudiantesConRespaldo(schoolId)
                val leidos = lectura.datos.mapIndexed { i, r ->
                    ediciones[r.id] ?: EstudianteClasificacion(
                        id = r.id,
                        numero = i + 1,
                        nombre = r.fullName,
                        grado = r.grade,
                        sexo = r.gender,
                        edad = r.age?.toString().orEmpty(),
                        muestra = r.sample,
                        nivelActual = if (r.retired) NIVEL_RETIRADO else r.niveles[materiaClave],
                        ultimaEvaluacion = r.fechasNivel[materiaClave]
                    )
                }
                // Las evaluaciones hechas sin conexión siguen viéndose aunque la lista venga del respaldo
                val estudiantes = aplicarPendientes(leidos, repo.pendientes(materiaClave))
                _uiState.update { it.copy(cargando = false, estudiantes = estudiantes, guardadoEn = lectura.guardadoEn) }
                // Un reintento de la misma institución sigue la sesión que ya iba corriendo
                if (sesion?.schoolId != schoolId) abrirSesion(schoolId, estudiantes.size)
            } catch (e: CancellationException) {
                throw e   // el docente cambió de institución antes de que llegara la lista
            } catch (e: Exception) {
                _uiState.update { it.copy(cargando = false, mensaje = mensajeDe(e)) }
            }
        }
    }

    private fun mensajeGuardado(e: Exception): String = when ((e as? CloudFunctionsApi.ApiException)?.code) {
        "unauthenticated" -> "Tu sesión expiró. Vuelve a iniciar sesión."
        "permission-denied" -> "No tienes acceso a este estudiante."
        "invalid-argument" -> "Revisa la edad, el sexo y la muestra antes de guardar."
        null -> "No pudimos guardar. Revisa tu conexión."
        else -> e.message ?: "No pudimos guardar."
    }

    private fun mensajeDe(e: Exception): String = when ((e as? CloudFunctionsApi.ApiException)?.code) {
        "no-school" -> "No tienes instituciones asignadas."
        "unauthenticated" -> "Tu sesión expiró. Vuelve a iniciar sesión."
        "permission-denied" -> "No tienes acceso a esta institución."
        null -> "No pudimos cargar los estudiantes. Revisa tu conexión."
        else -> e.message ?: "No pudimos cargar los estudiantes."
    }
}

/**
 * BQ 9: una pasada del docente por la lista de una institución. Empieza cuando la lista
 * aparece y termina en la última clasificación (no cuando sale de la pantalla), así el rato
 * que se queda mirando al final no infla el tiempo. Solo cuenta el tiempo en primer plano.
 */
internal class SesionClasificacion(val schoolId: String, private val estudiantesEnLista: Int) {
    private val clientId = UUID.randomUUID().toString()
    private val inicioMs = System.currentTimeMillis()
    private var activoMs = 0L
    private var reanudadaEn: Long? = null   // elapsedRealtime; null = pausada
    private var activoHastaUltimaMs = 0L
    private var ultimaClasificacionMs = 0L
    private val clasificados = mutableSetOf<String>()

    private fun activoAhora(): Long = activoMs + (reanudadaEn?.let { SystemClock.elapsedRealtime() - it } ?: 0L)

    fun reanudar() {
        if (reanudadaEn == null) reanudadaEn = SystemClock.elapsedRealtime()
    }

    fun pausar() {
        activoMs = activoAhora()
        reanudadaEn = null
    }

    /** Volver a clasificar al mismo estudiante no lo cuenta dos veces, pero sí alarga el tiempo. */
    fun registrarClasificacion(estudianteId: String) {
        clasificados += estudianteId
        activoHastaUltimaMs = activoAhora()
        ultimaClasificacionMs = System.currentTimeMillis()
    }

    /** Cuerpo de POST /classificationSessions, o null si no clasificó a nadie. */
    fun aJson(materia: String, versionApp: String): JSONObject? {
        if (clasificados.isEmpty()) return null
        return JSONObject()
            .put("clientId", clientId)
            .put("schoolId", schoolId)
            .put("subject", materia)
            .put("platform", InfoDispositivo.PLATAFORMA)
            .put("appVersion", versionApp)
            .put("startedAt", Instant.ofEpochMilli(inicioMs).toString())
            .put("endedAt", Instant.ofEpochMilli(ultimaClasificacionMs).toString())
            .put("activeSeconds", ((activoHastaUltimaMs + 999) / 1000).coerceAtLeast(1))
            .put("studentsClassified", clasificados.size)
            .put("studentsInList", maxOf(estudiantesEnLista, clasificados.size))
    }
}
