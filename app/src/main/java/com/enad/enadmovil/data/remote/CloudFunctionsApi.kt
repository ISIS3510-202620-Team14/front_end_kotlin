package com.enad.enadmovil.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Llama directo a las Cloud Functions register/login que ya existen en firebase_backend.
 */
object CloudFunctionsApi {

    private const val BASE_URL = "BASE_URL"

    data class RespuestaAuth(
        val uid: String,
        val rol: String,
        val customToken: String
    )

    class ApiException(val code: String, message: String) : Exception(message)

    suspend fun register(email: String, password: String, fullName: String): RespuestaAuth =
        llamar("register", mapOf("email" to email, "password" to password, "fullName" to fullName))

    suspend fun login(email: String, password: String): RespuestaAuth =
        llamar("login", mapOf("email" to email, "password" to password))

    private suspend fun llamar(ruta: String, cuerpo: Map<String, String>): RespuestaAuth =
        withContext(Dispatchers.IO) {
            val url = URL("$BASE_URL/$ruta")
            val conexion = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            val json = JSONObject(cuerpo).toString()
            conexion.outputStream.use { it.write(json.toByteArray()) }

            val codigo = conexion.responseCode
            val stream = if (codigo in 200..299) conexion.inputStream else conexion.errorStream
            val texto = stream.bufferedReader().use { it.readText() }
            val respuesta = JSONObject(texto)

            if (codigo !in 200..299) {
                val error = respuesta.getJSONObject("error")
                throw ApiException(error.getString("code"), error.getString("message"))
            }

            RespuestaAuth(
                uid = respuesta.getString("uid"),
                rol = respuesta.getString("rol"),
                customToken = respuesta.getString("customToken")
            )
        }
}