package com.enad.enadmovil.data.auth

import com.enad.enadmovil.data.remote.CloudFunctionsApi
import com.enad.enadmovil.data.repository.AuthRepository
import com.enad.enadmovil.domain.model.Usuario

/**
 * Facade de autenticación. Iniciar sesión de verdad son tres pasos sobre tres subsistemas:
 *  1. POST /login (Cloud Function): valida correo y contraseña contra Identity Toolkit y
 *     devuelve un customToken.
 *  2. signInWithCustomToken (Firebase Auth en el cliente): abre la sesión con ese token.
 *  3. users/{uid} (Firestore): carga la escuela del usuario.
 * La pantalla solo ve iniciarSesion(email, password).
 */
class AuthFacade(
    private val repo: AuthRepository = AuthRepository()
) {

    suspend fun iniciarSesion(email: String, password: String): Usuario {
        val respuesta = CloudFunctionsApi.login(email, password)
        repo.abrirSesion(respuesta.customToken)
        return Usuario(
            uid = respuesta.uid,
            email = email,
            rol = respuesta.rol,
            schoolId = repo.schoolIdDe(respuesta.uid)
        )
    }
}
