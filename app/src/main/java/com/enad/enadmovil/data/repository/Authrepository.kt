package com.enad.enadmovil.data.repository

import com.enad.enadmovil.data.remote.CloudFunctionsApi
import com.enad.enadmovil.data.remote.FirebaseModule
import com.enad.enadmovil.domain.model.Usuario
import kotlinx.coroutines.tasks.await

/**
 * Facade: hides the two real steps of login/register (calling the Cloud
 * Function + signing in with the customToken in Firebase Auth) behind a
 * single function.
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

    /**
     ID token to send as "Authorization: Bearer <token>" to any protected
     Cloud Function (e.g. /students, which checks this with verifyIdToken).
     Different from the customToken used only once at login/register.
     */
    suspend fun obtenerIdToken(): String? =
        FirebaseModule.auth.currentUser?.getIdToken(false)?.await()?.token
}