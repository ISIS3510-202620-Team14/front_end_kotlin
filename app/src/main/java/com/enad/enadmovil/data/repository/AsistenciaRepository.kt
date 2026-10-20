package com.enad.enadmovil.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.enad.enadmovil.data.local.AppDatabase
import com.enad.enadmovil.data.local.entity.AsistenciaEntity
import com.enad.enadmovil.data.local.entity.EstadoRemoto
import com.enad.enadmovil.data.local.entity.EstudianteEntity
import com.enad.enadmovil.data.local.entity.SyncStatus
import com.enad.enadmovil.data.remote.CloudFunctionsApi
import com.enad.enadmovil.data.sync.ControlSincronizacion
import com.enad.enadmovil.data.sync.SyncScheduler
import com.enad.enadmovil.domain.model.EstudianteEscaneado
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.withPermit
import java.io.IOException
import java.util.UUID

/**
 * Offline-first: la app siempre lee y escribe en Room. Lo pendiente se sube
 * al backend (Cloud Function students) cuando hay internet.
 */
class AsistenciaRepository(context: Context) {

    private val appContext = context.applicationContext
    private val db = AppDatabase.obtener(appContext)
    private val estudiantes = db.estudianteDao()
    private val asistencias = db.asistenciaDao()
    private val auth = AuthRepository()

    data class ResultadoImportacion(val nuevos: Int, val existentes: Int, val sinGrado: Int)

    /** Resultado de una sincronización de asistencia. */
    sealed interface ResultadoSync {
        /** Otra sincronización tenía el turno en este celular; esta no corrió. */
        data object Ocupado : ResultadoSync
        /** [conflictos]: marcas que no se enviaron porque otro celular ya había guardado otro valor. */
        data class Terminado(val enviadas: Int, val conflictos: Int, val quedanPendientes: Boolean, val aviso: String?) : ResultadoSync
    }

    fun observarEstudiantes(schoolId: String): Flow<List<EstudianteEntity>> = estudiantes.observar(schoolId)
    fun observarSemana(desde: String, hasta: String): Flow<List<AsistenciaEntity>> = asistencias.observarRango(desde, hasta)
    fun observarPendientes(uid: String): Flow<Int> = asistencias.contarPendientes(uid)

    /** Marcas que siguen sin subir, contadas por fecha (Mis datos · Asistencia por enviar). */
    suspend fun pendientesPorFecha(uid: String): Map<String, Int> =
        asistencias.pendientes(uid).groupingBy { it.fecha }.eachCount().toSortedMap()

    // ---------- Escritura local (siempre funciona, con o sin internet) ----------

    /** Antes de la primera marca local se guarda lo que había en el servidor (ver ControlSincronizacion). */
    private suspend fun recordarBase(estudianteId: String, fecha: String) {
        val previo = asistencias.buscar(estudianteId, fecha)
        if (previo == null || previo.syncStatus == SyncStatus.SYNCED) {
            ControlSincronizacion.recordarBase(appContext, estudianteId, fecha, previo?.estado ?: EstadoRemoto.SIN_REGISTRO)
        }
    }

    suspend fun marcar(estudianteId: String, fecha: String, estado: String, uid: String) {
        recordarBase(estudianteId, fecha)
        asistencias.upsert(AsistenciaEntity(estudianteId, fecha, estado, uid, SyncStatus.PENDING))
        SyncScheduler.programar(appContext)
    }

    /** "Limpiar": solo toca a quienes ya tienen una marca ese día. */
    suspend fun limpiarDia(ids: List<String>, fecha: String, uid: String) {
        val conMarca = asistencias.delDia(fecha)
            .filter { it.estudianteId in ids && it.estado != EstadoRemoto.SIN_REGISTRO }
            .map { it.copy(estado = EstadoRemoto.SIN_REGISTRO, docenteUid = uid, syncStatus = SyncStatus.PENDING) }
        if (conMarca.isEmpty()) return
        for (m in conMarca) recordarBase(m.estudianteId, m.fecha)
        asistencias.upsertAll(conMarca)
        SyncScheduler.programar(appContext)
    }

    /**
     * Guarda la lista escaneada. Sin código → provisional (el servidor asigna PROV-XXXX).
     * Sin grado en la fila se usa el del filtro de curso; si tampoco hay, se omite y se avisa.
     */
    suspend fun guardarLista(
        schoolId: String,
        uid: String,
        filas: List<EstudianteEscaneado>,
        gradoPorDefecto: Int?
    ): ResultadoImportacion {
        var nuevos = 0
        var existentes = 0
        var sinGrado = 0

        for (fila in filas) {
            val grado = fila.grado ?: gradoPorDefecto
            if (grado == null) { sinGrado++; continue }

            val codigo = fila.codigo?.trim()
            val previo = if (codigo != null) {
                estudiantes.porCodigo(schoolId, codigo)
            } else {
                estudiantes.porNombre(schoolId, fila.nombre.trim(), grado)
            }
            if (previo != null) { existentes++; continue }

            estudiantes.upsert(
                EstudianteEntity(
                    id = UUID.randomUUID().toString(),
                    schoolId = schoolId,
                    docenteUid = uid,
                    fullName = fila.nombre.trim(),
                    code = codigo,
                    grade = grado,
                    age = fila.edad,
                    provisional = codigo == null,
                    syncStatus = SyncStatus.PENDING
                )
            )
            nuevos++
        }
        if (nuevos > 0) SyncScheduler.programar(appContext)
        return ResultadoImportacion(nuevos, existentes, sinGrado)
    }

    /** "Estudiante inesperado" nuevo: provisional y ya marcado como presente ese día. */
    suspend fun crearInesperado(schoolId: String, uid: String, nombre: String, edad: Int?, grado: Int, fecha: String) {
        val id = UUID.randomUUID().toString()
        db.withTransaction {
            estudiantes.upsert(
                EstudianteEntity(
                    id = id, schoolId = schoolId, docenteUid = uid, fullName = nombre.trim(),
                    code = null, grade = grado, age = edad?.takeIf { it in 5..99 },
                    provisional = true, syncStatus = SyncStatus.PENDING
                )
            )
            asistencias.upsert(AsistenciaEntity(id, fecha, EstadoRemoto.VINO, uid, SyncStatus.PENDING))
        }
        SyncScheduler.programar(appContext)
    }

    // ---------- Bajar del backend ----------

    /**
     * Para cada día de la semana: GET /students?schoolId=...&date=... Actualiza el roster y las
     * marcas sin pisar lo que está pendiente. Hoy son 5 llamadas (el backend filtra por un día).
     */
    suspend fun descargarSemana(schoolId: String, uid: String, fechas: List<String>): Boolean {
        val token = runCatching { auth.obtenerToken() }.getOrNull() ?: return false
        var alguno = false
        for (fecha in fechas) {
            try {
                val remotos = CloudFunctionsApi.listarEstudiantes(token, schoolId, fecha)
                val entidades = mutableListOf<EstudianteEntity>()
                val marcas = mutableListOf<AsistenciaEntity>()

                for (r in remotos) {
                    if (estudiantes.porId(r.id)?.syncStatus != SyncStatus.PENDING) {
                        entidades += EstudianteEntity(
                            id = r.id, schoolId = r.schoolId, docenteUid = uid, fullName = r.fullName,
                            code = r.code, grade = r.grade, age = r.age, provisional = r.provisional,
                            syncStatus = SyncStatus.SYNCED
                        )
                    }
                    if ((r.attendance == EstadoRemoto.VINO || r.attendance == EstadoRemoto.NO_VINO) &&
                        asistencias.buscar(r.id, fecha)?.syncStatus != SyncStatus.PENDING
                    ) {
                        marcas += AsistenciaEntity(r.id, fecha, r.attendance, uid, SyncStatus.SYNCED)
                    }
                }
                db.withTransaction {
                    estudiantes.upsertAll(entidades)
                    asistencias.upsertAll(marcas)
                }
                alguno = true
            } catch (e: Exception) {
                // sin internet o error del servidor ese día: se sigue con lo que hay en el teléfono
            }
        }
        return alguno
    }

    // ---------- Subir al backend ----------

    /** Para el SyncWorker: true si no queda nada por reintentar (los errores definitivos se marcan FAILED). */
    suspend fun sincronizarPendientes(): Boolean = when (val r = sincronizarAhora()) {
        ResultadoSync.Ocupado -> false
        is ResultadoSync.Terminado -> {
            r.aviso?.let { ControlSincronizacion.guardarAviso(appContext, it) }
            !r.quedanPendientes
        }
    }

    /**
     * Sube estudiantes nuevos y asistencia. Usa el semáforo [ControlSincronizacion.turno] sin esperar:
     * si otra sincronización de este celular lo tiene, devuelve [ResultadoSync.Ocupado].
     */
    suspend fun sincronizarAhora(): ResultadoSync {
        if (!ControlSincronizacion.turno.tryAcquire()) return ResultadoSync.Ocupado
        try {
            return subirTodo()
        } finally {
            ControlSincronizacion.turno.release()
        }
    }

    private suspend fun subirTodo(): ResultadoSync.Terminado {
        val uid = auth.uidActual() ?: return ResultadoSync.Terminado(0, 0, false, null)
        val token = runCatching { auth.obtenerToken() }.getOrNull()
            ?: return ResultadoSync.Terminado(0, 0, true, null)
        var todoBien = true

        // 1) Estudiantes nuevos (POST con clientId = id local: reintentar no duplica).
        for (e in estudiantes.pendientes(uid)) {
            val grado = e.grade ?: continue
            try {
                val remoto = CloudFunctionsApi.crearEstudiante(token, e.schoolId, e.id, e.fullName, grado, e.age, e.code)
                estudiantes.actualizarSync(e.id, SyncStatus.SYNCED, remoto.code)
            } catch (ex: CloudFunctionsApi.ApiException) {
                if (ex.status == 409) {
                    if (!resolverConflicto(token, e)) estudiantes.actualizarSync(e.id, SyncStatus.FAILED, null)
                } else if (ex.esTransitorio) {
                    todoBien = false
                } else {
                    estudiantes.actualizarSync(e.id, SyncStatus.FAILED, null)
                }
            } catch (ex: IOException) {
                todoBien = false
            }
        }

        // 2) Asistencia: por colegio y día se lee lo que ya tiene el servidor y luego se suben las
        //    marcas en paralelo, máximo 4 a la vez (semáforo envios).
        val listas = mutableListOf<Pair<AsistenciaEntity, EstudianteEntity>>()
        for (a in asistencias.pendientes(uid)) {
            val est = estudiantes.porId(a.estudianteId)
            if (est == null || est.syncStatus == SyncStatus.PENDING) continue // su estudiante aún no sube
            if (est.syncStatus == SyncStatus.FAILED) {
                asistencias.actualizarSync(a.estudianteId, a.fecha, a.estado, SyncStatus.FAILED)
                continue
            }
            listas += a to est
        }

        var enviadas = 0
        val enConflicto = mutableListOf<Pair<EstudianteEntity, String>>()
        for ((grupo, marcas) in listas.groupBy { (a, est) -> est.schoolId to a.fecha }) {
            val (schoolId, fecha) = grupo
            val enServidor = try {
                CloudFunctionsApi.listarEstudiantes(token, schoolId, fecha).associate { it.id to (it.attendance ?: EstadoRemoto.SIN_REGISTRO) }
            } catch (ex: Exception) {
                todoBien = false
                continue // sin poder comparar no se sube nada de ese día
            }
            val resultados = coroutineScope {
                marcas.map { (a, est) ->
                    async {
                        ControlSincronizacion.envios.withPermit { subirMarca(token, a, est, enServidor[a.estudianteId] ?: EstadoRemoto.SIN_REGISTRO) }
                    }
                }.awaitAll()
            }
            resultados.forEach { r ->
                when (r) {
                    is Subida.Enviada -> enviadas++
                    is Subida.Conflicto -> enConflicto += r.estudiante to r.fecha
                    Subida.Reintentar -> todoBien = false
                    Subida.Fallida -> Unit
                }
            }
        }
        return ResultadoSync.Terminado(enviadas, enConflicto.size, !todoBien, avisoConflictos(enConflicto))
    }

    private sealed interface Subida {
        data object Enviada : Subida
        data class Conflicto(val estudiante: EstudianteEntity, val fecha: String) : Subida
        data object Reintentar : Subida
        data object Fallida : Subida
    }

    /**
     * Si el servidor ya no tiene el valor que había cuando el docente marcó, otro celular llegó
     * primero: no se pisa su marca, se adopta la del servidor y se avisa.
     */
    private suspend fun subirMarca(token: String, a: AsistenciaEntity, est: EstudianteEntity, servidor: String): Subida {
        val base = ControlSincronizacion.base(appContext, a.estudianteId, a.fecha) ?: servidor
        if (servidor != base && servidor != a.estado) {
            asistencias.upsert(a.copy(estado = servidor, syncStatus = SyncStatus.SYNCED))
            ControlSincronizacion.olvidarBase(appContext, a.estudianteId, a.fecha)
            return Subida.Conflicto(est, a.fecha)
        }
        return try {
            CloudFunctionsApi.marcarAsistencia(token, a.estudianteId, a.fecha, a.estado)
            asistencias.actualizarSync(a.estudianteId, a.fecha, a.estado, SyncStatus.SYNCED)
            ControlSincronizacion.olvidarBase(appContext, a.estudianteId, a.fecha)
            Subida.Enviada
        } catch (ex: CloudFunctionsApi.ApiException) {
            if (ex.esTransitorio) {
                Subida.Reintentar
            } else {
                asistencias.actualizarSync(a.estudianteId, a.fecha, a.estado, SyncStatus.FAILED)
                Subida.Fallida
            }
        } catch (ex: IOException) {
            Subida.Reintentar
        }
    }

    private fun avisoConflictos(conflictos: List<Pair<EstudianteEntity, String>>): String? {
        if (conflictos.isEmpty()) return null
        val nombres = conflictos.take(3).joinToString(", ") { (est, fecha) -> "${est.fullName} ($fecha)" }
        val resto = if (conflictos.size > 3) " y ${conflictos.size - 3} más" else ""
        return "Otro celular registró primero la asistencia de ${conflictos.size} estudiante(s): $nombres$resto. " +
            "Tus marcas no se enviaron para no pisar las suyas; ahora ves las del servidor. Revísalas y corrígelas si hace falta."
    }

    /**
     * 409: ese código ya existe en el servidor (p. ej. otro celular lo subió antes).
     * Se adopta el estudiante del servidor y se reasignan sus asistencias locales.
     */
    private suspend fun resolverConflicto(token: String, local: EstudianteEntity): Boolean {
        val codigo = local.code ?: return false
        val remoto = CloudFunctionsApi.listarEstudiantes(token, local.schoolId, null).firstOrNull { it.code == codigo }
            ?: return false
        db.withTransaction {
            asistencias.reasignar(local.id, remoto.id)
            estudiantes.borrar(local.id)
            estudiantes.upsert(
                local.copy(
                    id = remoto.id, fullName = remoto.fullName, code = remoto.code, grade = remoto.grade,
                    age = remoto.age ?: local.age, provisional = remoto.provisional, syncStatus = SyncStatus.SYNCED
                )
            )
        }
        return true
    }
}