package com.enad.enadmovil.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.enad.enadmovil.data.repository.ClasificacionRepository
import java.util.concurrent.TimeUnit

/**
 * Manda las evaluaciones que se hicieron sin conexión (EvaluacionesPendientes). Solo corre con
 * internet; si el envío se corta, las que faltan siguen en la cola y se reintenta más tarde.
 */
class EnviarEvaluacionesWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val terminado = runCatching { ClasificacionRepository(applicationContext).enviarPendientes() }.getOrDefault(false)
        return when {
            terminado -> Result.success()
            runAttemptCount >= 8 -> Result.failure() // la próxima evaluación sin conexión programa otra ronda
            else -> Result.retry()
        }
    }

    companion object {
        private const val NOMBRE = "enviar_evaluaciones"

        fun programar(context: Context) {
            val peticion = OneTimeWorkRequestBuilder<EnviarEvaluacionesWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            // Si ya hay una ronda en cola, la nueva va después y también recoge lo que se agregó
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(NOMBRE, ExistingWorkPolicy.APPEND_OR_REPLACE, peticion)
        }
    }
}
