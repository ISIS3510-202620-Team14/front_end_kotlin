package com.enad.enadmovil.domain.model
object Rol {
    const val DOCENTE = "docente"
    const val ADMINISTRADOR = "admin" // must match with ROLES_CON_ACCESO (backend) in estudiantes/auth.js
    const val VOLUNTARIO = "voluntario"
    const val SIN_PRIVILEGIOS = "sin_privilegios"
}