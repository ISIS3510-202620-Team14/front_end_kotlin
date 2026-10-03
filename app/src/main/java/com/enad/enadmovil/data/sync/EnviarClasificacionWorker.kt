package com.enad.enadmovil.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.enad.enadmovil.data.remote.CloudFunctionsApi
import com.enad.enadmovil.data.repository.AuthRepository
import org.json.JSONObject

/**
 * BQ 9: manda una sesión de clasificación al backend. Si no hay internet, WorkManager la
 * guarda y la manda cuando vuelva; el clientId evita que un reintento la duplique.
 */
class EnviarClasificacionWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val sesion = inputData.getString(CLAVE_SESION)?.let { runCatching { JSONObject(it) }.getOrNull() }
            ?: return Result.failure()
        val token = AuthRepository().obtenerToken() ?: return reintentar()
        return try {
            CloudFunctionsApi.enviarSesionClasificacion(token, sesion)
            Result.success()
        } catch (e: CloudFunctionsApi.ApiException) {
            // Un 400/403/409 no se arregla reintentando
            if (e.esTransitorio) reintentar() else Result.failure()
        } catch (e: Exception) {
            reintentar()
        }
    }

    private fun reintentar(): Result = if (runAttemptCount >= 8) Result.failure() else Result.retry()

    companion object {
        private const val CLAVE_SESION = "sesion"

        fun programar(context: Context, sesion: JSONObject) {
            val peticion = OneTimeWorkRequestBuilder<EnviarClasificacionWorker>()
                .setInputData(workDataOf(CLAVE_SESION to sesion.toString()))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork("clasificacion_${sesion.getString("clientId")}", ExistingWorkPolicy.KEEP, peticion)
        }
    }
}
