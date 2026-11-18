package com.enad.enadmovil.data.local

import android.content.Context
import com.enad.enadmovil.domain.model.Usuario

/**
 * Datos del usuario que inició sesión, guardados en SharedPreferences. Firebase Auth ya
 * recuerda la sesión al cerrar la app, pero no el rol ni el nombre que devuelve el backend.
 * Con esto la app abre directo en la pantalla del rol sin volver a pedir el login.
 * Es temporal: dura [DURACION_MS] desde la última vez que se usó y después pide el login otra vez.
 * Se borra antes si cambia de usuario o cierra sesión.
 */
object SesionGuardada {
    private const val ARCHIVO = "enad_sesion"
    private const val UID = "uid"
    private const val EMAIL = "email"
    private const val NOMBRE = "nombre"
    private const val ROL = "rol"
    private const val SCHOOL_ID = "schoolId"
    private const val GUARDADA_EN = "guardadaEn"

    /** Cuánto dura la sesión guardada. Para la prueba son 5 minutos. */
    const val DURACION_MS = 5 * 60 * 1000L

    fun guardar(context: Context, usuario: Usuario) {
        prefs(context).edit()
            .putString(UID, usuario.uid)
            .putString(EMAIL, usuario.email)
            .putString(NOMBRE, usuario.fullName)
            .putString(ROL, usuario.rol)
            .putString(SCHOOL_ID, usuario.schoolId)
            .putLong(GUARDADA_EN, System.currentTimeMillis())
            .apply()
    }

    /** null si no hay sesión guardada o ya se venció. Una vencida se borra. */
    fun leer(context: Context): Usuario? {
        val p = prefs(context)
        if (System.currentTimeMillis() - p.getLong(GUARDADA_EN, 0L) > DURACION_MS) {
            borrar(context)
            return null
        }
        val uid = p.getString(UID, null) ?: return null
        val rol = p.getString(ROL, null) ?: return null
        return Usuario(
            uid = uid,
            email = p.getString(EMAIL, "").orEmpty(),
            fullName = p.getString(NOMBRE, "").orEmpty(),
            rol = rol,
            schoolId = p.getString(SCHOOL_ID, null)
        )
    }

    fun borrar(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
}
