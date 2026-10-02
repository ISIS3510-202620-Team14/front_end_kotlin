package com.enad.enadmovil.data.repository

import android.content.Context
import android.location.Location
import androidx.room.withTransaction
import com.enad.enadmovil.data.local.AppDatabase
import com.enad.enadmovil.data.local.entity.ContextoDiaEntity
import com.enad.enadmovil.data.local.entity.GrupoHorarioEntity
import com.enad.enadmovil.data.local.entity.PresenciaSedeEntity
import com.enad.enadmovil.data.local.entity.SedeEntity
import com.enad.enadmovil.data.location.LocationDataSource
import com.enad.enadmovil.data.remote.CloudFunctionsApi
import com.enad.enadmovil.domain.model.GrupoDelDia
import com.enad.enadmovil.domain.model.Horario
import java.time.LocalDate

/**
 * Context-aware: con una lectura de ubicación y el horario de los grupos sugiere el contexto del día.
 * Solo sugiere; el docente siempre confirma. Funciona sin señal con lo último descargado.
 */
class ContextoRepository(context: Context) {
    private val appContext = context.applicationContext
    private val db = AppDatabase.obtener(appContext)
    private val dao = db.contextoDao()
    private val ubicacion = LocationDataSource(appContext)
    private val auth = AuthRepository()

    companion object {
        const val RADIO_SEDE_M = 150f
        private const val MAX_IMPRECISION_M = 200f
    }

    fun tienePermisoUbicacion(): Boolean = ubicacion.tienePermiso()

    /** Descarga sedes y grupos con horario. Sin internet devuelve false y queda lo último guardado. */
    suspend fun descargarCatalogo(): Boolean {
        val uid = auth.uidActual() ?: return false
        val token = runCatching { auth.obtenerToken() }.getOrNull() ?: return false
        return try {
            val escuelas = CloudFunctionsApi.listarEscuelas(token)
            val grupos = CloudFunctionsApi.listarGruposDelDocente(token, uid)
            val sedes = escuelas.flatMap { escuela ->
                escuela.sedes.map { sede ->
                    SedeEntity(
                        id = "${escuela.id}/${sede.id}",
                        schoolId = escuela.id,
                        campusId = sede.id,
                        nombre = sede.nombre,
                        escuela = escuela.nombre,
                        lat = sede.lat,
                        lng = sede.lng
                    )
                }
            }
            db.withTransaction {
                dao.borrarSedes()
                dao.guardarSedes(sedes)
                dao.borrarGrupos(uid)
                dao.guardarGrupos(grupos.map { grupo ->
                    GrupoHorarioEntity(id = grupo.id, docenteUid = uid, nombre = grupo.nombre, materia = grupo.materia, schoolId = grupo.schoolId, campusId = grupo.campusId, horario = Horario.codificar(grupo.horario))
                })
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    /**
     * Una lectura de ubicación. Si el docente está en una sede, guarda la hora de hoy (primera y última)
     * y devuelve la sede. La posición se usa solo para comparar y no se guarda.
     */
    suspend fun registrarPresencia(ahora: Long = System.currentTimeMillis()): SedeEntity? {
        val uid = auth.uidActual() ?: return null
        val posicion = ubicacion.ubicacionActual() ?: return null
        if (posicion.hasAccuracy() && posicion.accuracy > MAX_IMPRECISION_M) return null
        val sede = sedeCercana(posicion.latitude, posicion.longitude) ?: return null
        val fecha = LocalDate.now().toString()
        val previa = dao.presencia(uid, fecha, sede.id)
        dao.guardarPresencia(PresenciaSedeEntity(uid, fecha, sede.id, previa?.primeraVez ?: ahora, ahora))
        return sede
    }

    private suspend fun sedeCercana(lat: Double, lng: Double): SedeEntity? {
        val distancia = FloatArray(1)
        return dao.sedesUbicadas()
            .map { sede ->
                Location.distanceBetween(lat, lng, sede.lat!!, sede.lng!!, distancia)
                sede to distancia[0]
            }
            .filter { (_, metros) ->
                metros <= RADIO_SEDE_M
            }
            .minByOrNull { (_, metros) -> metros }
            ?.first
    }
    /** Grupo que hoy se dicta en esa sede, si hay alguno planeado. */
    suspend fun sugerir(sede: SedeEntity?): GrupoDelDia? {
        val uid = auth.uidActual() ?: return null
        sede ?: return null
        val hoy = LocalDate.now()
        return dao.grupos(uid)
            .filter { it.schoolId == sede.schoolId && it.campusId == sede.campusId }
            .map { aGrupoDelDia(it, hoy) }
            .firstOrNull { it.horasPlaneadas != null}
    }
    /** Grupo que el docente ya confirmó hoy, si lo hizo. */
    suspend fun contextoDeHoy(): GrupoDelDia? {
        val uid = auth.uidActual() ?: return null
        val hoy = LocalDate.now()
        val confirmado = dao.contexto(uid, hoy.toString()) ?: return null
        return dao.grupo(confirmado.grupoId)?.let { aGrupoDelDia(it, hoy) }
    }

    suspend fun gruposDelDocente(): List<GrupoDelDia> {
        val uid = auth.uidActual() ?: return emptyList()
        val hoy = LocalDate.now()
        return dao.grupos(uid).map { aGrupoDelDia(it, hoy)}
    }

    suspend fun confirmar(grupoId: String, origen: String) {
        val uid = auth.uidActual() ?: return
        dao.guardarContexto(ContextoDiaEntity(uid, LocalDate.now().toString(), grupoId, origen, System.currentTimeMillis()))
    }

    private suspend fun aGrupoDelDia(grupo: GrupoHorarioEntity, fecha: LocalDate) = GrupoDelDia(
        id = grupo.id,
        nombre = grupo.nombre,
        materia = grupo.materia,
        sede = grupo.campusId?.let { dao.sede("${grupo.schoolId}/$it")?.nombre },
        horasPlaneadas = Horario.horasDelDia(grupo.horario, fecha.dayOfWeek)
    )
}