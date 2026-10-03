package com.enad.enadmovil.data.remote

import com.enad.enadmovil.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Punto único de acceso a las instancias de Firebase (Singleton).
 * Nadie más en la app debe llamar FirebaseAuth.getInstance() directo.
 * Con USAR_EMULADORES (solo debug) ambos apuntan a los emuladores del computador.
 */
object FirebaseModule {
    private const val HOST_EMULADOR = "10.0.2.2"

    val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance().apply {
            if (BuildConfig.USAR_EMULADORES) useEmulator(HOST_EMULADOR, 9099)
        }
    }
    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance().apply {
            if (BuildConfig.USAR_EMULADORES) useEmulator(HOST_EMULADOR, 8080)
        }
    }
}
