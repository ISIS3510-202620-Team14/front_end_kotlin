package com.enad.enadmovil.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.enad.enadmovil.data.repository.AsistenciaRepository

class SyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val terminado = try {
            AsistenciaRepository(applicationContext).sincronizarPendientes()
        } catch (e: Exception) {
            false
        }
        return when {
            terminado -> Result.success()
            runAttemptCount >= 8 -> Result.failure() // el próximo cambio programa otra ronda
            else -> Result.retry()                    // backoff automático
        }
    }
}