package com.enad.enadmovil.domain.model

data class Usuario(
    val uid: String,
    val email: String,
    val fullName: String = "",
    val rol: String,
    val schoolId: String? = null   // null = aún sin colegio asignado (se asigna a partir del docente)
)