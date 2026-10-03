package com.enad.enadmovil.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await

/** Lectura puntual de ubicación en primer plano. Sin seguimiento ni ubicación en segundo plano. */
class LocationDataSource(context: Context) {
    private val appContext = context.applicationContext
    private val cliente = LocationServices.getFusedLocationProviderClient(appContext)

    fun tienePermiso(): Boolean = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
        ContextCompat.checkSelfPermission(appContext, it) == PackageManager.PERMISSION_GRANTED
    }

    /** Una sola lectura. null si no hay permiso, el GPS está apagado o no responde a tiempo. */
    @SuppressLint("MissingPermission")
    suspend fun ubicacionActual(): Location? {
        if (!tienePermiso()) return null
        val solicitud = CurrentLocationRequest.Builder().setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(MAX_ANTIGUEDAD_MS)
            .setDurationMillis(TIEMPO_MAXIMO_MS)
            .build()
        return try {
            cliente.getCurrentLocation(solicitud, null).await()
        } catch (e: Exception) {
            null
        }
    }

    private companion object {
        const val MAX_ANTIGUEDAD_MS = 60_000L //Acepta lectura menor a 1 minuto
        const val TIEMPO_MAXIMO_MS = 20_000L //Si en 20s no hay señal, se sigue sin ubicación.
    }
}