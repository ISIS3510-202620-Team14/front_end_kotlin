package com.enad.enadmovil.data.repository

import com.enad.enadmovil.data.remote.CloudFunctionsApi
import com.enad.enadmovil.data.remote.FirebaseModule
import kotlinx.coroutines.tasks.await

/**
 * Repository de la sesión: abre y cierra la sesión de Firebase Auth, lee la escuela del
 * perfil en users/{uid} y entrega el ID token que piden los endpoints. Login y registro
 * completos (varios pasos) los orquesta AuthFacade.
 */
class AuthRepository {

    /** Abre la sesión de Firebase Auth en el cliente con el customToken que emitió el backend. */
    suspend fun abrirSesion(customToken: String) {
        FirebaseModule.auth.signInWithCustomToken(customToken).await()
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