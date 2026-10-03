package com.enad.enadmovil.data.repository

import com.enad.enadmovil.data.remote.CloudFunctionsApi
import com.enad.enadmovil.data.remote.FirebaseModule
import com.enad.enadmovil.domain.model.Usuario
import kotlinx.coroutines.tasks.await

/**
 * Facade: hides the two real steps of login/register (calling the Cloud
 * Function + signing in with the customToken in Firebase Auth) behind a
 * single function. Also loads the user's schoolId from users/{uid} and
 * exposes the ID token the students endpoints need.
 */
class AuthRepository {

    /** Resultado del registro: el usuario y si el back le envió el correo de bienvenida. */
    data class Registro(val usuario: Usuario, val correoBienvenidaEnviado: Boolean)

    /** Abre la sesión de Firebase Auth en el cliente con el customToken que emitió el backend. */
    suspend fun abrirSesion(customToken: String) {
        FirebaseModule.auth.signInWithCustomToken(customToken).await()
    }

    /** [sedesPorInstitucion]: id de institución -> ids de sedes elegidas en ella. */
    suspend fun registrarse(
        email: String,
        password: String,
        fullName: String,
        sedesPorInstitucion: Map<String, List<String>>
    ): Registro {
        val respuesta = CloudFunctionsApi.register(email, password, fullName, sedesPorInstitucion)
        FirebaseModule.auth.signInWithCustomToken(respuesta.customToken).await()
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

    /** Instituciones activas con sus sedes que se pueden elegir al registrarse (no pide sesión). */
    suspend fun institucionesRegistro(): List<CloudFunctionsApi.Institucion> =
        CloudFunctionsApi.institucionesRegistro()

    fun cerrarSesion() {
        FirebaseModule.auth.signOut()
    }

    fun usuarioActual() = FirebaseModule.auth.currentUser

    fun uidActual(): String? = FirebaseModule.auth.currentUser?.uid

    /** schoolId del docente con sesión abierta (Firestore lo cachea, así que funciona sin internet). */
    suspend fun schoolIdActual(): String? = uidActual()?.let { schoolIdDe(it) }

    /** ID token para el header Authorization: Bearer. Firebase lo renueva solo cuando vence. */
    suspend fun obtenerToken(): String? =
        FirebaseModule.auth.currentUser?.getIdToken(false)?.await()?.token

    /**
     * El backend ahora guarda las escuelas del docente en `schoolIds` (lista); los perfiles
     * viejos traen un solo `schoolId`. Se leen ambos. Con varias escuelas se usa la primera
     * (falta un selector de escuela).
     */
    suspend fun schoolIdDe(uid: String): String? =
        runCatching {
            val doc = FirebaseModule.firestore
                .collection("users")
                .document(uid)
                .get()
                .await()
            val lista = (doc.get("schoolIds") as? List<*>)?.filterIsInstance<String>().orEmpty()
            lista.firstOrNull() ?: doc.getString("schoolId")
        }.getOrNull()
}