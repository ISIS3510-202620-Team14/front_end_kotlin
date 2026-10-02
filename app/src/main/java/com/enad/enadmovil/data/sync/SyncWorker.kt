package com.enad.enadmovil.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.enad.enadmovil.data.repository.AsistenciaRepository
import com.enad.enadmovil.data.repository.SesionAgrupacionRepository

class SyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val asistencia = try {
            AsistenciaRepository(applicationContext).sincronizarPendientes()
        } catch (e: Exception) {
            false
        }
        val agrupaciones = try {
            SesionAgrupacionRepository(applicationContext).sincronizarPendientes()
        } catch (e: Exception) {
            false
        }
        val terminado = asistencia && agrupaciones
        return when {
            terminado -> Result.success()
            runAttemptCount >= 8 -> Result.failure() // el próximo cambio programa otra ronda
            else -> Result.retry()                    // backoff automático
        }
    }
}