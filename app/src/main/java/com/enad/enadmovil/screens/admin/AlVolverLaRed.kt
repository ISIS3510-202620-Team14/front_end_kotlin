package com.enad.enadmovil.ui.screens.admin

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build

class AlVolverLaRed(context: Context, private val alVolver: () -> Unit) {
    private val conectividad = context.getSystemService(ConnectivityManager::class.java)

    @Volatile
    var esperando = false

    private val aviso = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            if (!esperando) return
            esperando = false
            alVolver()
        }
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            runCatching { conectividad?.registerDefaultNetworkCallback(aviso) }
        }
    }

    fun cerrar() {
        runCatching { conectividad?.unregisterNetworkCallback(aviso) }
    }
}