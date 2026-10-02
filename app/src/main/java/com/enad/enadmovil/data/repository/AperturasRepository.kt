package com.enad.enadmovil.data.repository

import android.content.Context
import com.enad.enadmovil.data.local.AppDatabase
import com.enad.enadmovil.data.local.entity.AperturaEntity
import com.enad.enadmovil.data.local.entity.SyncStatus
import com.enad.enadmovil.data.remote.CloudFunctionsApi
import com.enad.enadmovil.data.sync.SyncScheduler
import com.enad.enadmovil.data.telemetria.InfoDispositivo
import com.enad.enadmovil.domain.Semana
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.io.IOException
import java.util.UUID

/** Offline-first: opens are written to Room first and uploaded later by SyncWorker. */
class AperturasRepository(context: Context) {

    private val appContext = context.applicationContext
    private val aperturas = AppDatabase.obtener(appContext).aperturaDao()
    private val auth = AuthRepository()

    companion object {
        // Opens closer than this count as the same usage session
        private const val BRECHA_MIN_MS = 5 * 60 * 1000L
        private const val LOTE = 100
    }

    suspend fun registrar() {
        val uid = auth.uidActual() ?: return
        val ahora = System.currentTimeMillis()
        val ultima = aperturas.ultimaApertura(uid)
        if (ultima != null && ahora - ultima < BRECHA_MIN_MS) return

        aperturas.insertar(
            AperturaEntity(
                id = UUID.randomUUID().toString(),
                docenteUid = uid,
                abiertaEn = ahora,
                semana = Semana.id(),
                platform = InfoDispositivo.PLATAFORMA,
                appVersion = InfoDispositivo.versionApp(appContext),
                syncStatus = SyncStatus.PENDING
            )
        )
        SyncScheduler.programar(appContext)
    }

    fun observarSemana(): Flow<Int> {
        val uid = auth.uidActual() ?: return flowOf(0)
        return aperturas.observarDeLaSemana(uid, Semana.id())
    }

    /** Returns true when there is nothing left to retry. */
    suspend fun sincronizarPendientes(): Boolean {
        val uid = auth.uidActual() ?: return true
        val pendientes = aperturas.pendientes(uid)
        if (pendientes.isEmpty()) return true
        val token = runCatching { auth.obtenerToken() }.getOrNull() ?: return false

        var todoBien = true
        for (lote in pendientes.chunked(LOTE)) {
            val ids = lote.map { it.id }
            try {
                CloudFunctionsApi.enviarAperturas(token, lote)
                aperturas.marcar(ids, SyncStatus.SYNCED)
            } catch (e: CloudFunctionsApi.ApiException) {
                if (e.esTransitorio) todoBien = false
                else aperturas.marcar(ids, SyncStatus.FAILED)
            } catch (e: IOException) {
                todoBien = false
            }
        }
        return todoBien
    }
}