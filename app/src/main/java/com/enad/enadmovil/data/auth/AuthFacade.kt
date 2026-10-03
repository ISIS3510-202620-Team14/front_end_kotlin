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

    /** Resultado del registro: el usuario y si el back le envió el correo de bienvenida. */
    data class Registro(val usuario: Usuario, val correoBienvenidaEnviado: Boolean)

    /**
     * Mismos pasos que [iniciarSesion], pero con POST /register (crea la cuenta y su perfil).
     * [sedesPorInstitucion]: id de institución -> ids de sedes elegidas en ella.
     */
    suspend fun registrarse(
        email: String,
        password: String,
        fullName: String,
        sedesPorInstitucion: Map<String, List<String>>
    ): Registro {
        val respuesta = CloudFunctionsApi.register(email, password, fullName, sedesPorInstitucion)
        repo.abrirSesion(respuesta.customToken)
        val usuario = Usuario(
            uid = respuesta.uid,
            email = email,
            fullName = fullName,
            rol = respuesta.rol,
            // Igual que al iniciar sesión: con varias escuelas se usa la primera.
            schoolId = sedesPorInstitucion.keys.firstOrNull()
        )
        return Registro(usuario, respuesta.welcomeEmailSent)
    }
}
