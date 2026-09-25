package com.enad.enadmovil.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Punto único de acceso a las instancias de Firebase (Singleton).
 * Nadie más en la app debe llamar FirebaseAuth.getInstance() directo.
 */
object FirebaseModule {
    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
}