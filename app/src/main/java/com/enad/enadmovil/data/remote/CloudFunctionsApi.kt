package com.enad.enadmovil.data.remote

import com.enad.enadmovil.BuildConfig
import com.enad.enadmovil.data.local.entity.SesionAgrupacionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONArray
import com.enad.enadmovil.data.local.entity.AperturaEntity

/**
 * Llama directo a las Cloud Functions de firebase_backend:
 * register/login (sin token) y students (con Authorization: Bearer <idToken>).
 */
object CloudFunctionsApi {

    // Con USAR_EMULADORES (solo debug) va al emulador de Functions del computador:
    // 10.0.2.2 es el localhost del computador visto desde el emulador de Android.
    private val BASE_URL = if (BuildConfig.USAR_EMULADORES) {
        "http://10.0.2.2:5001/enad-movil/us-central1"
    } else {
        "https://us-central1-enad-movil.cloudfunctions.net"
    }
    private val STUDENTS_URL = "$BASE_URL/students"
    private val GROUPINGS_URL = "$BASE_URL/groupings"
    private val OPENS_URL = "$BASE_URL/appOpens"
    private val SCHOOLS_URL = "$BASE_URL/schools"
    private val CLASSIFICATIONS_URL = "$BASE_URL/classificationSessions"

    data class RespuestaAuth(
        val uid: String,
        val rol: String,
        val customToken: String,
        val welcomeEmailSent: Boolean = false   // solo lo manda register
    )

    data class Sede(val id: String, val name: String)

    /** Institución que se puede elegir al registrarse, con sus sedes. */
    data class Institucion(
        val id: String,
        val name: String,
        val municipality: String?,
        val campuses: List<Sede>
    )

    data class EstudianteRemoto(
        val id: String,
        val schoolId: String,
        val code: String,
        val fullName: String,
        val grade: Int,
        val age: Int?,
        val provisional: Boolean,
        val attendance: String?,   // solo viene si se pidió ?date=
        val gender: String? = null,
        val niveles: Map<String, String> = emptyMap()   // materia -> etiqueta del nivel actual
    )
    suspend fun enviarAperturas(token: String, aperturas: List<AperturaEntity>) {
        val arreglo = JSONArray()
        aperturas.forEach { a ->
            arreglo.put(
                JSONObject()
                    .put("clientId", a.id)
                    .put("openedAt", java.time.Instant.ofEpochMilli(a.abiertaEn).toString())
                    .put("week", a.semana)
                    .put("platform", a.platform)
                    .put("appVersion", a.appVersion)
            )
        }
        solicitar("POST", "", token, JSONObject().put("opens", arreglo), OPENS_URL)
    }

    data class ReporteAperturas(
        val semana: String,
        val docentes: Int,
        val docentesMasDeUna: Int,
        val provisional: Boolean
    )

    /** GET /appOpens/report (admin only): teachers that opened the app more than once in a week. */
    suspend fun reporteAperturas(token: String, semana: String? = null): ReporteAperturas {
        val ruta = if (semana != null) "/report?week=$semana" else "/report"
        val json = solicitar("GET", ruta, token, null, OPENS_URL)
        return ReporteAperturas(
            semana = json.getString("week"),
            docentes = json.getInt("teachers"),
            docentesMasDeUna = json.getInt("teachersOverOnce"),
            provisional = json.optBoolean("provisional", false)
        )
    }
    /** status = código HTTP (0 si no aplica). */
    class ApiException(val code: String, message: String, val status: Int = 0) : Exception(message) {
        /** Errores que valen la pena reintentar más tarde. */
        val esTransitorio: Boolean get() = status >= 500 || status == 429 || status == 408 || status == 401
    }

    // ---------- Auth ----------

    /**
     * POST /register. [sedesPorInstitucion]: id de institución -> ids de las sedes elegidas en ella.
     * Si va vacío, la cuenta se crea sin escuela y un admin la asigna después.
     */
    suspend fun register(
        email: String,
        password: String,
        fullName: String,
        sedesPorInstitucion: Map<String, List<String>> = emptyMap()
    ): RespuestaAuth {
        val schools = JSONArray()
        sedesPorInstitucion.forEach { (schoolId, campusIds) ->
            schools.put(JSONObject().put("schoolId", schoolId).put("campusIds", JSONArray(campusIds)))
        }
        return llamar(
            "register",
            JSONObject()
                .put("email", email)
                .put("password", password)
                .put("fullName", fullName)
                .put("schools", schools)
        )
    }

    suspend fun login(email: String, password: String): RespuestaAuth =
        llamar("login", JSONObject().put("email", email).put("password", password))

    /** GET /schools: al docente solo le llegan sus instituciones; al admin, todas. */
    suspend fun misInstituciones(token: String): List<Institucion> {
        val arreglo = solicitar("GET", "", token, null, SCHOOLS_URL).getJSONArray("schools")
        return List(arreglo.length()) { aInstitucion(arreglo.getJSONObject(it)) }
    }

    private fun aInstitucion(o: JSONObject): Institucion {
        val sedes = o.optJSONArray("campuses") ?: JSONArray()
        return Institucion(
            id = o.getString("id"),
            name = o.getString("name"),
            municipality = if (o.isNull("municipality")) null else o.optString("municipality"),
            campuses = List(sedes.length()) { j ->
                val sede = sedes.getJSONObject(j)
                Sede(sede.getString("id"), sede.getString("name"))
            }
        )
    }

    /** GET /registerSchools (sin token): instituciones activas con sus sedes, para el registro. */
    suspend fun institucionesRegistro(): List<Institucion> =
        withContext(Dispatchers.IO) {
            val conexion = (URL("$BASE_URL/registerSchools").openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 20_000
            }
            try {
                val codigo = conexion.responseCode
                if (codigo !in 200..299) {
                    throw ApiException("http-$codigo", "No pudimos cargar las instituciones. Intenta más tarde.", codigo)
                }
                val arreglo = JSONObject(conexion.inputStream.bufferedReader().use { it.readText() })
                    .getJSONArray("schools")
                List(arreglo.length()) { aInstitucion(arreglo.getJSONObject(it)) }
            } finally {
                conexion.disconnect()
            }
        }

    private suspend fun llamar(ruta: String, cuerpo: JSONObject): RespuestaAuth =
        withContext(Dispatchers.IO) {
            val url = URL("$BASE_URL/$ruta")
            val conexion = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            conexion.outputStream.use { it.write(cuerpo.toString().toByteArray(Charsets.UTF_8)) }

            val codigo = conexion.responseCode
            val stream = if (codigo in 200..299) conexion.inputStream else conexion.errorStream
            val texto = stream.bufferedReader().use { it.readText() }
            val respuesta = JSONObject(texto)

            if (codigo !in 200..299) {
                val error = respuesta.getJSONObject("error")
                throw ApiException(error.getString("code"), error.getString("message"), codigo)
            }

            RespuestaAuth(
                uid = respuesta.getString("uid"),
                rol = respuesta.getString("rol"),
                customToken = respuesta.getString("customToken"),
                welcomeEmailSent = respuesta.optBoolean("welcomeEmailSent", false)
            )
        }

    // ---------- Estudiantes ----------

    /** POST /students. Con clientId, repetir el envío devuelve el mismo estudiante (200) en vez de duplicarlo. */
    suspend fun crearEstudiante(
        token: String,
        schoolId: String,
        clientId: String,
        fullName: String,
        grade: Int,
        age: Int?,
        code: String?
    ): EstudianteRemoto {
        val cuerpo = JSONObject()
            .put("schoolId", schoolId) // obligatorio si el docente tiene varias escuelas
            .put("clientId", clientId)
            .put("fullName", fullName)
            .put("grade", grade)
        if (age != null) cuerpo.put("age", age)
        if (code != null) cuerpo.put("code", code) else cuerpo.put("provisional", true)
        return aEstudiante(solicitar("POST", "", token, cuerpo))
    }

    /** PUT /students/{id}/attendance con estado vino | no_vino | sin_registro. */
    suspend fun marcarAsistencia(token: String, id: String, fecha: String, estado: String) {
        solicitar("PUT", "/$id/attendance", token, JSONObject().put("date", fecha).put("status", estado))
    }

    /** GET /students (del colegio del docente). Con fecha, cada estudiante trae su asistencia de ese día. */
    suspend fun listarEstudiantes(token: String, schoolId: String?, fecha: String?): List<EstudianteRemoto> {
        val params = listOfNotNull(
            schoolId?.let { "schoolId=${URLEncoder.encode(it, "UTF-8")}" },
            fecha?.let { "date=$it" }
        )
        val ruta = if (params.isEmpty()) "" else "?" + params.joinToString("&")
        val arreglo = solicitar("GET", ruta, token, null).getJSONArray("students")
        return List(arreglo.length()) { aEstudiante(arreglo.getJSONObject(it)) }
    }

    private fun aEstudiante(o: JSONObject) = EstudianteRemoto(
        id = o.getString("id"),
        schoolId = o.getString("schoolId"),
        code = o.getString("code"),
        fullName = o.getString("fullName"),
        grade = o.getInt("grade"),
        age = if (o.isNull("age")) null else o.getInt("age"),
        provisional = o.optBoolean("provisional", false),
        attendance = if (o.has("attendance")) o.optString("attendance") else null,
        gender = if (o.isNull("gender")) null else o.optString("gender"),
        niveles = buildMap {
            val levels = o.optJSONObject("levels") ?: return@buildMap
            levels.keys().forEach { materia ->
                levels.optJSONObject(materia)?.optString("etiqueta")?.ifBlank { null }?.let { put(materia, it) }
            }
        }
    )

    suspend fun enviarSesionAgrupacion(token: String, s: SesionAgrupacionEntity, intento: Int) {
        val cuerpo = JSONObject().put("clientId", s.id)
            .put("platform", s.platform)
            .put("appVersion", s.appVersion)
            .put("connectivity", s.connectivity)
            .put("createdAt", java.time.Instant.ofEpochMilli(s.creadaEn).toString())
            .put("sentAt", java.time.Instant.now().toString())
            .put("attempt", intento)
            .put("subject", s.subject)
            .put("groupName", s.groupName)
            .put("studentsCounted", s.studentsCounted)
            .put("teachersCounted", s.teachersCounted)
            .put("studentsAssigned", s.studentsAssigned)
        if (s.schoolId != null) cuerpo.put("schoolId", s.schoolId)
        solicitar("POST", "", token, cuerpo, GROUPINGS_URL)
    }

    /** Una fila del reporte. En el total de la semana, plataforma, versión y conectividad vienen en null. */
    data class FilaReporte(
        val semana: String,
        val plataforma: String?,
        val version: String?,
        val conectividad: String?,
        val total: Int,
        val aTiempo: Int,
        val tarde: Int,
        val errores: Int,
        val porcentaje: Double?,
        val provisional: Boolean
    )

    data class ReporteSincronizacion(val semanas: List<FilaReporte>, val filas: List<FilaReporte>)

    /** GET /groupings/report (solo admin): % de sesiones que no llegaron en 24 h o terminaron en error. */
    suspend fun reporteSesionesAgrupacion(token: String): ReporteSincronizacion {
        val json = solicitar("GET", "/report", token, null, GROUPINGS_URL)
        fun filas(nombre: String): List<FilaReporte> {
            val arreglo = json.getJSONArray(nombre)
            return List(arreglo.length()) { aFilaReporte(arreglo.getJSONObject(it)) }
        }
        return ReporteSincronizacion(semanas = filas("weeks"), filas = filas("rows"))
    }

    private fun aFilaReporte(o: JSONObject) = FilaReporte(
        semana = o.getString("week"),
        plataforma = o.optString("platform").ifBlank { null },
        version = o.optString("appVersion").ifBlank { null },
        conectividad = o.optString("connectivity").ifBlank { null },
        total = o.getInt("total"),
        aTiempo = o.getInt("syncedOnTime"),
        tarde = o.getInt("syncedLate"),
        errores = o.getInt("errors"),
        porcentaje = if (o.isNull("percentage")) null else o.getDouble("percentage"),
        provisional = o.optBoolean("provisional", false)
    )

    // ---------- BQ 9: tiempo de clasificación ----------

    /** POST /classificationSessions. El cuerpo lo arma ClasificacionViewModel; con el mismo clientId no se duplica. */
    suspend fun enviarSesionClasificacion(token: String, sesion: JSONObject) {
        solicitar("POST", "", token, sesion, CLASSIFICATIONS_URL)
    }

    data class DocenteClasificacion(
        val nombre: String?,
        val sesiones: Int,
        val estudiantes: Int,
        val minutosPor25: Double
    )

    data class InstitucionClasificacion(
        val nombre: String,
        val sesiones: Int,
        val estudiantes: Int,
        val promedioMinutosPor25: Double,
        val docentes: List<DocenteClasificacion>
    )

    /** GET /classificationSessions/report (solo admin): minutos por cada 25 estudiantes, por institución y docente. */
    suspend fun reporteClasificacion(token: String, materia: String? = null): List<InstitucionClasificacion> {
        val ruta = if (materia != null) "/report?subject=$materia" else "/report"
        val escuelas = solicitar("GET", ruta, token, null, CLASSIFICATIONS_URL).getJSONArray("schools")
        return List(escuelas.length()) { i ->
            val e = escuelas.getJSONObject(i)
            val docentes = e.getJSONArray("teachers")
            InstitucionClasificacion(
                nombre = e.getString("name"),
                sesiones = e.getInt("sessions"),
                estudiantes = e.getInt("studentsClassified"),
                promedioMinutosPor25 = e.getDouble("avgMinutesPer25"),
                docentes = List(docentes.length()) { j ->
                    val d = docentes.getJSONObject(j)
                    DocenteClasificacion(
                        nombre = if (d.isNull("fullName")) null else d.getString("fullName"),
                        sesiones = d.getInt("sessions"),
                        estudiantes = d.getInt("studentsClassified"),
                        minutosPor25 = d.getDouble("minutesPer25")
                    )
                }
            )
        }
    }

    private suspend fun solicitar(metodo: String, ruta: String, token: String, cuerpo: JSONObject?, base: String = STUDENTS_URL): JSONObject =
        withContext(Dispatchers.IO) {
            val conexion = (URL("$base$ruta").openConnection() as HttpURLConnection).apply {
                requestMethod = metodo
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                if (cuerpo != null) doOutput = true
            }
            try {
                if (cuerpo != null) conexion.outputStream.use { it.write(cuerpo.toString().toByteArray(Charsets.UTF_8)) }
                val codigo = conexion.responseCode
                val stream = if (codigo in 200..299) conexion.inputStream else conexion.errorStream
                val texto = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val json = runCatching { JSONObject(texto) }.getOrDefault(JSONObject())
                if (codigo !in 200..299) {
                    val error = json.optJSONObject("error")
                    throw ApiException(
                        code = error?.optString("code")?.ifBlank { null } ?: "http-$codigo",
                        message = error?.optString("message")?.ifBlank { null } ?: "Error $codigo",
                        status = codigo
                    )
                }
                json
            } finally {
                conexion.disconnect()
            }
        }
}