package com.enad.enadmovil.data.repository

import com.enad.enadmovil.data.remote.CloudFunctionsApi
import com.enad.enadmovil.data.remote.FirebaseModule
import com.enad.enadmovil.domain.model.Usuario
import kotlinx.coroutines.tasks.await

/**
 * Pattern Facade: oculta los dos pasos reales del login/registro (llamar la Cloud Function
 * + abrir sesión con el customToken en Firebase Auth) detrás de una sola función.
 */
class AuthRepository {

    suspend fun iniciarSesion(email: String, password: String): Usuario {
        val respuesta = CloudFunctionsApi.login(email, password)
        FirebaseModule.auth.signInWithCustomToken(respuesta.customToken).await()
        return Usuario(uid = respuesta.uid, email = email, rol = respuesta.rol)
    }

    suspend fun registrarse(email: String, password: String, fullName: String): Usuario {
        val respuesta = CloudFunctionsApi.register(email, password, fullName)
        FirebaseModule.auth.signInWithCustomToken(respuesta.customToken).await()
        return Usuario(uid = respuesta.uid, email = email, fullName = fullName, rol = respuesta.rol)
    }

    fun cerrarSesion() {
        FirebaseModule.auth.signOut()
    }

    fun usuarioActual() = FirebaseModule.auth.currentUser
}