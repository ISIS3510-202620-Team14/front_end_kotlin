package com.enad.enadmovil.data.niveles

import com.enad.enadmovil.data.remote.CloudFunctionsApi
import com.enad.enadmovil.data.repository.AuthRepository
import org.json.JSONObject

/** Una institución de la lista, lo que hace falta para pedir sus estudiantes y mostrarla. */
data class EscuelaNivel(val id: String, val nombre: String)

/** De dónde salen las respuestas que se guardan en la caché: el texto JSON tal como llega. */
interface FuenteNiveles {
    /** GET /schools */
    suspend fun escuelasJson(): String

    /** GET /students?schoolId= */
    suspend fun estudiantesJson(schoolId: String): String
}

/** El backend, con el token del usuario que tiene la sesión abierta. */
class FuenteNivelesRed(private val auth: AuthRepository = AuthRepository()) : FuenteNiveles {

    override suspend fun escuelasJson(): String = CloudFunctionsApi.misInstitucionesJson(token()).toString()

    override suspend fun estudiantesJson(schoolId: String): String =
        CloudFunctionsApi.listarEstudiantesJson(token(), schoolId, null).toString()

    private suspend fun token(): String =
        auth.obtenerToken() ?: throw CloudFunctionsApi.ApiException("unauthenticated", "Tu sesión expiró. Vuelve a iniciar sesión.", 401)
}

/** Lee la lista de GET /schools: id y nombre de cada institución. */
fun leerEscuelas(escuelasJson: String): List<EscuelaNivel> {
    val arreglo = JSONObject(escuelasJson).optJSONArray("schools") ?: return emptyList()
    return (0 until arreglo.length()).mapNotNull { i ->
        val o = arreglo.optJSONObject(i) ?: return@mapNotNull null
        val id = o.optString("id").ifBlank { return@mapNotNull null }
        EscuelaNivel(id, o.optString("name").ifBlank { "Sin nombre" })
    }
}
