package com.enad.enadmovil.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.enad.enadmovil.data.local.AppDatabase
import com.enad.enadmovil.data.local.entity.AsistenciaEntity
import com.enad.enadmovil.data.local.entity.EstadoRemoto
import com.enad.enadmovil.data.local.entity.EstudianteEntity
import com.enad.enadmovil.data.local.entity.SyncStatus
import com.enad.enadmovil.data.remote.CloudFunctionsApi
import com.enad.enadmovil.data.sync.SyncScheduler
import com.enad.enadmovil.domain.model.EstudianteEscaneado
import kotlinx.coroutines.flow.Flow
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

    fun observarEstudiantes(schoolId: String): Flow<List<EstudianteEntity>> = estudiantes.observar(schoolId)
    fun observarAsistencia(fecha: String): Flow<List<AsistenciaEntity>> = asistencias.observarDelDia(fecha)
    fun observarPendientes(uid: String): Flow<Int> = asistencias.contarPendientes(uid)

    // ---------- Escritura local (siempre funciona, con o sin internet) ----------

    suspend fun marcar(estudianteId: String, fecha: String, estado: String, uid: String) {
        asistencias.upsert(AsistenciaEntity(estudianteId, fecha, estado, uid, SyncStatus.PENDING))
        SyncScheduler.programar(appContext)
    }

    /** "Limpiar": solo toca a quienes ya tienen una marca ese día. */
    suspend fun limpiarDia(ids: List<String>, fecha: String, uid: String) {
        val conMarca = asistencias.delDia(fecha)
            .filter { it.estudianteId in ids && it.estado != EstadoRemoto.SIN_REGISTRO }
            .map { it.copy(estado = EstadoRemoto.SIN_REGISTRO, docenteUid = uid, syncStatus = SyncStatus.PENDING) }
        if (conMarca.isEmpty()) return
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

    /** GET /students?date=...: actualiza el roster y la asistencia del día sin pisar lo que está pendiente. */
    suspend fun descargarRoster(schoolId: String, uid: String, fecha: String): Boolean {
        val token = runCatching { auth.obtenerToken() }.getOrNull() ?: return false
        return try {
            val remotos = CloudFunctionsApi.listarEstudiantes(token, fecha)
            val entidades = mutableListOf<EstudianteEntity>()
            val marcas = mutableListOf<AsistenciaEntity>()

            for (r in remotos) {
                if (estudiantes.porId(r.id)?.syncStatus != SyncStatus.PENDING) {
                    entidades += EstudianteEntity(
                        id = r.id, schoolId = schoolId, docenteUid = uid, fullName = r.fullName,
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
            true
        } catch (e: Exception) {
            false // sin internet o error del servidor: se sigue con lo que hay en el teléfono
        }
    }

    // ---------- Subir al backend ----------

    /** Devuelve true si no queda nada por reintentar (los errores definitivos se marcan FAILED). */
    suspend fun sincronizarPendientes(): Boolean {
        val uid = auth.uidActual() ?: return true
        val token = runCatching { auth.obtenerToken() }.getOrNull() ?: return false
        var todoBien = true

        // 1) Estudiantes nuevos (POST con clientId = id local: reintentar no duplica).
        for (e in estudiantes.pendientes(uid)) {
            val grado = e.grade ?: continue
            try {
                val remoto = CloudFunctionsApi.crearEstudiante(token, e.id, e.fullName, grado, e.age, e.code)
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

        // 2) Asistencia (PUT por estudiante y día).
        for (a in asistencias.pendientes(uid)) {
            val est = estudiantes.porId(a.estudianteId)
            if (est == null || est.syncStatus == SyncStatus.PENDING) continue // su estudiante aún no sube
            if (est.syncStatus == SyncStatus.FAILED) {
                asistencias.actualizarSync(a.estudianteId, a.fecha, a.estado, SyncStatus.FAILED)
                continue
            }
            try {
                CloudFunctionsApi.marcarAsistencia(token, a.estudianteId, a.fecha, a.estado)
                asistencias.actualizarSync(a.estudianteId, a.fecha, a.estado, SyncStatus.SYNCED)
            } catch (ex: CloudFunctionsApi.ApiException) {
                if (ex.esTransitorio) todoBien = false
                else asistencias.actualizarSync(a.estudianteId, a.fecha, a.estado, SyncStatus.FAILED)
            } catch (ex: IOException) {
                todoBien = false
            }
        }
        return todoBien
    }

    /**
     * 409: ese código ya existe en el servidor (p. ej. otro celular lo subió antes).
     * Se adopta el estudiante del servidor y se reasignan sus asistencias locales.
     */
    private suspend fun resolverConflicto(token: String, local: EstudianteEntity): Boolean {
        val codigo = local.code ?: return false
        val remoto = CloudFunctionsApi.listarEstudiantes(token, null).firstOrNull { it.code == codigo }
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