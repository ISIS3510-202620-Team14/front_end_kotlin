package com.enad.enadmovil.data.telemetria

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat

object InfoDispositivo {
    const val PLATAFORMA = "kotlin"
    const val ONLINE = "online"
    const val OFFLINE = "offline"

    fun conectividad(context: Context): String {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return OFFLINE
        val capacidades = cm.getNetworkCapabilities(cm.activeNetwork) ?: return OFFLINE
        val conInternet = capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        return if (conInternet) ONLINE else OFFLINE
    }

    fun versionApp(context: Context): String {
        val pm = context.packageManager
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(context.packageName, 0)
        }
        return "${info.versionName ?: "0"} (${PackageInfoCompat.getLongVersionCode(info)})"
    }
}