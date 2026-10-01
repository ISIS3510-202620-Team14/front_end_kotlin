package com.enad.enadmovil.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

object SyncScheduler {
    private const val NOMBRE = "sync_asistencia"

    /** Encola una sincronización. Si no hay internet queda esperando y corre sola cuando vuelva. */
    fun programar(context: Context) {
        val peticion = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(NOMBRE, ExistingWorkPolicy.APPEND_OR_REPLACE, peticion)
    }
}